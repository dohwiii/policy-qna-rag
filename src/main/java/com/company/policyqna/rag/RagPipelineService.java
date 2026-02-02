package com.company.policyqna.rag;

import com.company.policyqna.ontology.OntologyService;
import com.company.policyqna.ontology.OntologyService.QueryExpansion;
import com.company.policyqna.ontology.OntologyService.RedirectResult;
import com.company.policyqna.ontology.OntologyService.TermDefinition;
import com.company.policyqna.vector.VectorStoreService;
import com.company.policyqna.vector.VectorStoreService.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 파이프라인 서비스
 * 1. 질문 분석 (온톨로지 활용)
 * 2. 검색어 확장
 * 3. 벡터 검색 + 키워드 검색
 * 4. 온톨로지 기반 재순위
 * 5. LLM 답변 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagPipelineService {

    private final VectorStoreService vectorStoreService;
    private final OntologyService ontologyService;
    private final ChatClient.Builder chatClientBuilder;

    @Value("${rag.top-k:5}")
    private int topK;

    @Value("${rag.use-ontology-boost:true}")
    private boolean useOntologyBoost;

    @Value("${rag.ontology-boost-weight:0.3}")
    private double ontologyBoostWeight;

    private static final String SYSTEM_PROMPT = """
        당신은 사내 정책 및 업무 매뉴얼 전문 상담 AI입니다.

        ## 역할
        - 제공된 문서 컨텍스트를 기반으로 정확하고 신뢰할 수 있는 답변을 제공합니다.
        - 반드시 출처(문서명, 조항번호)를 명시합니다.
        - 문서에 없는 내용은 추측하지 않고 "해당 정보를 찾을 수 없습니다"라고 답합니다.

        ## 답변 형식
        1. 핵심 답변 (간결하게)
        2. 상세 설명 (필요시)
        3. 출처 정보
        4. 관련 규정/참고사항 (있는 경우)

        ## 주의사항
        - 법적 효력이 있는 답변이 아님을 인지합니다.
        - 최신 정보는 담당 부서에 확인하도록 안내합니다.
        """;

    /**
     * 질문에 대한 답변 생성
     */
    public QnaResponse processQuestion(String question) {
        log.info("Processing question: {}", question);

        // 1. 리다이렉트 규칙 확인
        Optional<RedirectResult> redirect = ontologyService.checkRedirect(question);
        if (redirect.isPresent()) {
            log.info("Redirect rule matched: {}", redirect.get().getRuleName());
            // 리다이렉트된 참조로 직접 검색
            return processWithRedirect(question, redirect.get());
        }

        // 2. 온톨로지 기반 쿼리 확장
        QueryExpansion expansion = ontologyService.expandQuery(question);
        log.debug("Query expanded: {} terms", expansion.getExpandedTerms().size());

        // 3. 벡터 검색 수행
        List<SearchResult> searchResults;
        if (useOntologyBoost && !expansion.getExpandedTerms().isEmpty()) {
            searchResults = vectorStoreService.searchWithExpansion(
                expansion.getExpandedTerms(),
                expansion.getTermWeights(),
                topK
            );
        } else {
            searchResults = vectorStoreService.search(question, topK);
        }

        // 4. 검색 결과가 없는 경우
        if (searchResults.isEmpty()) {
            return QnaResponse.builder()
                .question(question)
                .answer("죄송합니다. 질문과 관련된 정책이나 매뉴얼 정보를 찾을 수 없습니다. " +
                        "다른 키워드로 검색하거나 담당 부서에 문의해 주세요.")
                .sources(Collections.emptyList())
                .relatedTerms(Collections.emptyList())
                .build();
        }

        // 5. 컨텍스트 구성
        String context = buildContext(searchResults);

        // 6. 관련 용어 정의 수집
        List<TermInfo> relatedTerms = extractRelatedTerms(question, expansion);

        // 7. LLM 답변 생성
        String answer = generateAnswer(question, context, relatedTerms);

        // 8. 출처 정보 추출
        List<SourceInfo> sources = extractSources(searchResults);

        return QnaResponse.builder()
            .question(question)
            .answer(answer)
            .sources(sources)
            .relatedTerms(relatedTerms)
            .expandedTerms(expansion.getExpandedTerms())
            .searchScores(searchResults.stream()
                .collect(Collectors.toMap(
                    SearchResult::getChunkId,
                    SearchResult::getScore
                )))
            .build();
    }

    /**
     * 리다이렉트된 질문 처리
     */
    private QnaResponse processWithRedirect(String question, RedirectResult redirect) {
        // 리다이렉트 대상으로 직접 검색
        List<SearchResult> results = vectorStoreService.search(redirect.getTargetReference(), topK);

        if (results.isEmpty()) {
            return QnaResponse.builder()
                .question(question)
                .answer("'" + redirect.getTargetReference() + "'을(를) 참조하도록 설정되어 있으나, " +
                        "해당 문서를 찾을 수 없습니다.")
                .sources(Collections.emptyList())
                .redirectInfo(redirect)
                .build();
        }

        String context = buildContext(results);
        String answer = generateAnswer(question, context, Collections.emptyList());

        return QnaResponse.builder()
            .question(question)
            .answer(answer)
            .sources(extractSources(results))
            .redirectInfo(redirect)
            .build();
    }

    /**
     * 검색 결과로 컨텍스트 구성
     */
    private String buildContext(List<SearchResult> results) {
        StringBuilder context = new StringBuilder();
        context.append("=== 관련 문서 내용 ===\n\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            context.append(String.format("[문서 %d] %s\n", i + 1, result.getSourceReference()));
            context.append(result.getContent());
            context.append("\n\n---\n\n");
        }

        return context.toString();
    }

    /**
     * 관련 용어 정의 추출
     */
    private List<TermInfo> extractRelatedTerms(String question, QueryExpansion expansion) {
        List<TermInfo> terms = new ArrayList<>();

        for (var concept : expansion.getMatchedConcepts()) {
            Optional<TermDefinition> definition = ontologyService.getTermDefinition(concept.getName());
            definition.ifPresent(def -> terms.add(TermInfo.builder()
                .term(def.getTerm())
                .definition(def.getDefinition())
                .conceptType(def.getConceptType().getKoreanName())
                .synonyms(def.getSynonyms())
                .build()));
        }

        return terms;
    }

    /**
     * LLM을 통한 답변 생성
     */
    private String generateAnswer(String question, String context, List<TermInfo> relatedTerms) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## 질문\n").append(question).append("\n\n");
        prompt.append("## 참조 문서\n").append(context).append("\n");

        if (!relatedTerms.isEmpty()) {
            prompt.append("## 관련 용어 정의\n");
            for (TermInfo term : relatedTerms) {
                prompt.append("- ").append(term.getTerm()).append(": ")
                      .append(term.getDefinition()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("위 문서를 참고하여 질문에 답변해 주세요.");

        ChatClient chatClient = chatClientBuilder.build();

        List<Message> messages = List.of(
            new SystemMessage(SYSTEM_PROMPT),
            new UserMessage(prompt.toString())
        );

        return chatClient.prompt(new Prompt(messages))
            .call()
            .content();
    }

    /**
     * 출처 정보 추출
     */
    private List<SourceInfo> extractSources(List<SearchResult> results) {
        return results.stream()
            .map(r -> SourceInfo.builder()
                .documentTitle(r.getDocumentTitle())
                .documentCode(r.getDocumentCode())
                .sectionTitle(r.getSectionTitle())
                .articleNumber(r.getArticleNumber())
                .relevanceScore(r.getScore())
                .snippet(truncate(r.getContent(), 200))
                .build())
            .collect(Collectors.toList());
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    // ==================== 응답 DTO ====================

    @lombok.Builder
    @lombok.Getter
    public static class QnaResponse {
        private String question;
        private String answer;
        private List<SourceInfo> sources;
        private List<TermInfo> relatedTerms;
        private List<String> expandedTerms;
        private Map<String, Double> searchScores;
        private RedirectResult redirectInfo;
    }

    @lombok.Builder
    @lombok.Getter
    public static class SourceInfo {
        private String documentTitle;
        private String documentCode;
        private String sectionTitle;
        private String articleNumber;
        private double relevanceScore;
        private String snippet;

        public String getFullReference() {
            StringBuilder ref = new StringBuilder();
            if (documentTitle != null) ref.append(documentTitle);
            if (documentCode != null) ref.append(" (").append(documentCode).append(")");
            if (articleNumber != null) ref.append(" ").append(articleNumber);
            return ref.toString();
        }
    }

    @lombok.Builder
    @lombok.Getter
    public static class TermInfo {
        private String term;
        private String definition;
        private String conceptType;
        private List<String> synonyms;
    }
}
