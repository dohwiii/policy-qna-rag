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
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

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

    // ==================== 공개 API ====================

    /**
     * 동기 답변 생성 (기존 /ask 엔드포인트용)
     */
    public QnaResponse processQuestion(String question) {
        log.info("Processing question: {}", question);
        RagContext ctx = buildRagContext(question);

        if (ctx.noResults()) {
            if (ctx.redirectInfo() != null) {
                return QnaResponse.builder()
                    .question(question)
                    .answer("'" + ctx.redirectInfo().getTargetReference() + "'을(를) 참조하도록 설정되어 있으나, " +
                            "해당 문서를 찾을 수 없습니다.")
                    .sources(Collections.emptyList())
                    .redirectInfo(ctx.redirectInfo())
                    .build();
            }
            return QnaResponse.builder()
                .question(question)
                .answer("죄송합니다. 질문과 관련된 정책이나 매뉴얼 정보를 찾을 수 없습니다. " +
                        "다른 키워드로 검색하거나 담당 부서에 문의해 주세요.")
                .sources(Collections.emptyList())
                .relatedTerms(Collections.emptyList())
                .build();
        }

        String answer = generateAnswer(ctx.promptText());
        return QnaResponse.builder()
            .question(question)
            .answer(answer)
            .sources(ctx.sources())
            .relatedTerms(ctx.relatedTerms())
            .expandedTerms(ctx.expandedTerms())
            .searchScores(ctx.searchScores())
            .redirectInfo(ctx.redirectInfo())
            .build();
    }

    /**
     * RAG 파이프라인 1~6단계: LLM 호출을 제외한 모든 처리.
     * sync/stream 양쪽에서 공유하는 메서드.
     */
    public RagContext buildRagContext(String question) {
        // 1. 리다이렉트 규칙 확인
        Optional<RedirectResult> redirect = ontologyService.checkRedirect(question);
        if (redirect.isPresent()) {
            RedirectResult redirectInfo = redirect.get();
            log.info("Redirect rule matched: {}", redirectInfo.getRuleName());
            List<SearchResult> results = vectorStoreService.search(redirectInfo.getTargetReference(), topK);
            if (results.isEmpty()) {
                return new RagContext(question, Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyMap(), redirectInfo, null, true);
            }
            String context = buildContext(results);
            String promptText = buildPromptText(question, context, Collections.emptyList());
            return new RagContext(question, extractSources(results), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyMap(), redirectInfo, promptText, false);
        }

        // 2. 온톨로지 기반 쿼리 확장
        QueryExpansion expansion = ontologyService.expandQuery(question);
        log.debug("Query expanded: {} terms", expansion.getExpandedTerms().size());

        // 3. 벡터 검색
        List<SearchResult> searchResults;
        if (useOntologyBoost && !expansion.getExpandedTerms().isEmpty()) {
            searchResults = vectorStoreService.searchWithExpansion(
                expansion.getExpandedTerms(), expansion.getTermWeights(), topK);
        } else {
            searchResults = vectorStoreService.search(question, topK);
        }

        // 4. 검색 결과 없음
        if (searchResults.isEmpty()) {
            return new RagContext(question, Collections.emptyList(), Collections.emptyList(),
                expansion.getExpandedTerms(), Collections.emptyMap(), null, null, true);
        }

        // 5. 컨텍스트 구성
        String context = buildContext(searchResults);

        // 6. 관련 용어 정의 수집 + 프롬프트 구성
        List<TermInfo> relatedTerms = extractRelatedTerms(question, expansion);
        String promptText = buildPromptText(question, context, relatedTerms);
        List<SourceInfo> sources = extractSources(searchResults);
        Map<String, Double> scores = searchResults.stream()
            .collect(Collectors.toMap(SearchResult::getChunkId, SearchResult::getScore));

        return new RagContext(question, sources, relatedTerms, expansion.getExpandedTerms(),
            scores, null, promptText, false);
    }

    /**
     * LLM 스트리밍 응답 (Flux<String>).
     * buildRagContext()로 컨텍스트를 준비한 후 이 메서드로 스트리밍.
     */
    public Flux<String> streamAnswer(RagContext ctx) {
        List<Message> messages = List.of(
            new SystemMessage(SYSTEM_PROMPT),
            new UserMessage(ctx.promptText())
        );
        return chatClientBuilder.build()
            .prompt(new Prompt(messages))
            .stream()
            .content();
    }

    // ==================== 내부 처리 메서드 ====================

    private String generateAnswer(String promptText) {
        ChatClient chatClient = chatClientBuilder.build();
        List<Message> messages = List.of(
            new SystemMessage(SYSTEM_PROMPT),
            new UserMessage(promptText)
        );
        return chatClient.prompt(new Prompt(messages)).call().content();
    }

    private String buildPromptText(String question, String context, List<TermInfo> relatedTerms) {
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
        return prompt.toString();
    }

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

    // ==================== DTO ====================

    public record RagContext(
        String question,
        List<SourceInfo> sources,
        List<TermInfo> relatedTerms,
        List<String> expandedTerms,
        Map<String, Double> searchScores,
        RedirectResult redirectInfo,
        String promptText,
        boolean noResults
    ) {}

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
