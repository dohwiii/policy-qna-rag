package com.company.policyqna.vector;

import com.company.policyqna.domain.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 벡터 스토어 서비스
 * - 문서 청크 임베딩 및 저장
 * - 유사도 검색
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    @Value("${rag.top-k:5}")
    private int defaultTopK;

    @Value("${rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    /**
     * 청크들을 벡터 스토어에 인덱싱
     */
    public void indexChunks(List<DocumentChunk> chunks) {
        List<Document> documents = chunks.stream()
            .map(this::toSpringAiDocument)
            .collect(Collectors.toList());

        vectorStore.add(documents);
        log.info("Indexed {} chunks to vector store", chunks.size());
    }

    /**
     * 단일 청크 인덱싱
     */
    public String indexChunk(DocumentChunk chunk) {
        Document doc = toSpringAiDocument(chunk);
        vectorStore.add(List.of(doc));
        return doc.getId();
    }

    /**
     * 유사도 검색
     */
    public List<SearchResult> search(String query, int topK) {
        SearchRequest request = SearchRequest.query(query)
            .withTopK(topK > 0 ? topK : defaultTopK)
            .withSimilarityThreshold(similarityThreshold);

        List<Document> results = vectorStore.similaritySearch(request);

        return results.stream()
            .map(this::toSearchResult)
            .collect(Collectors.toList());
    }

    /**
     * 확장된 검색 - 여러 쿼리 조합 (온톨로지 확장용)
     */
    public List<SearchResult> searchWithExpansion(
            List<String> queries,
            Map<String, Double> weights,
            int topK) {

        Map<String, SearchResult> resultMap = new HashMap<>();

        for (String query : queries) {
            double weight = weights.getOrDefault(query, 1.0);
            List<SearchResult> queryResults = search(query, topK);

            for (SearchResult result : queryResults) {
                String key = result.getChunkId();
                if (resultMap.containsKey(key)) {
                    // 기존 결과와 점수 병합
                    SearchResult existing = resultMap.get(key);
                    double newScore = existing.getScore() + (result.getScore() * weight);
                    resultMap.put(key, existing.toBuilder().score(newScore).build());
                } else {
                    resultMap.put(key, result.toBuilder()
                        .score(result.getScore() * weight)
                        .build());
                }
            }
        }

        // 점수순 정렬 후 상위 K개 반환
        return resultMap.values().stream()
            .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
            .limit(topK)
            .collect(Collectors.toList());
    }

    /**
     * 필터링 검색 (문서 타입, 부서 등)
     */
    public List<SearchResult> searchWithFilter(
            String query,
            Map<String, Object> filters,
            int topK) {

        // Spring AI의 filter expression 사용
        StringBuilder filterExpression = new StringBuilder();
        List<String> conditions = new ArrayList<>();

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            conditions.add(String.format("%s == '%s'", entry.getKey(), entry.getValue()));
        }

        if (!conditions.isEmpty()) {
            filterExpression.append(String.join(" && ", conditions));
        }

        SearchRequest request = SearchRequest.query(query)
            .withTopK(topK)
            .withSimilarityThreshold(similarityThreshold)
            .withFilterExpression(filterExpression.length() > 0 ? filterExpression.toString() : null);

        List<Document> results = vectorStore.similaritySearch(request);

        return results.stream()
            .map(this::toSearchResult)
            .collect(Collectors.toList());
    }

    /**
     * 문서ID로 벡터 삭제
     */
    public void deleteByDocumentId(Long documentId) {
        // PGVector에서는 metadata 기반 삭제 필요
        // 실제 구현은 VectorStore 구현체에 따라 다름
        vectorStore.delete(List.of("documentId:" + documentId));
        log.info("Deleted vectors for document: {}", documentId);
    }

    /**
     * DocumentChunk -> Spring AI Document 변환
     */
    private Document toSpringAiDocument(DocumentChunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("chunkId", chunk.getId());
        metadata.put("documentId", chunk.getDocument().getId());
        metadata.put("documentTitle", chunk.getDocument().getTitle());
        metadata.put("documentCode", chunk.getDocument().getDocumentCode());
        metadata.put("documentType", chunk.getDocument().getDocumentType().name());
        metadata.put("department", chunk.getDocument().getDepartment());
        metadata.put("sectionTitle", chunk.getSectionTitle());
        metadata.put("articleNumber", chunk.getArticleNumber());
        metadata.put("chunkIndex", chunk.getChunkIndex());

        if (chunk.getMetadata() != null) {
            metadata.putAll(chunk.getMetadata());
        }

        return new Document(
            String.valueOf(chunk.getId()),
            chunk.getContent(),
            metadata
        );
    }

    /**
     * Spring AI Document -> SearchResult 변환
     */
    private SearchResult toSearchResult(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();

        return SearchResult.builder()
            .chunkId(doc.getId())
            .content(doc.getContent())
            .score(getScoreFromMetadata(metadata))
            .documentId(getLongFromMetadata(metadata, "documentId"))
            .documentTitle((String) metadata.get("documentTitle"))
            .documentCode((String) metadata.get("documentCode"))
            .sectionTitle((String) metadata.get("sectionTitle"))
            .articleNumber((String) metadata.get("articleNumber"))
            .metadata(metadata)
            .build();
    }

    private double getScoreFromMetadata(Map<String, Object> metadata) {
        Object score = metadata.get("score");
        if (score instanceof Number) {
            return ((Number) score).doubleValue();
        }
        return 0.0;
    }

    private Long getLongFromMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    @lombok.Builder(toBuilder = true)
    @lombok.Getter
    public static class SearchResult {
        private String chunkId;
        private String content;
        private double score;
        private Long documentId;
        private String documentTitle;
        private String documentCode;
        private String sectionTitle;
        private String articleNumber;
        private Map<String, Object> metadata;

        /**
         * 출처 참조 문자열 생성
         */
        public String getSourceReference() {
            StringBuilder ref = new StringBuilder();
            if (documentTitle != null) ref.append(documentTitle);
            if (articleNumber != null) ref.append(" ").append(articleNumber);
            if (sectionTitle != null) ref.append(" (").append(sectionTitle).append(")");
            return ref.toString();
        }
    }
}
