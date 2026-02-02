package com.company.policyqna.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 문서 청크 - 벡터 검색 단위
 */
@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private PolicyDocument document;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "section_title")
    private String sectionTitle;

    @Column(name = "article_number")
    private String articleNumber;  // 예: 제15조, 3.2.1

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "start_offset")
    private Integer startOffset;

    @Column(name = "end_offset")
    private Integer endOffset;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "vector_id")
    private String vectorId;  // 벡터 스토어의 ID

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 출처 정보 생성
     */
    public String getSourceReference() {
        StringBuilder ref = new StringBuilder();
        ref.append(document.getTitle());

        if (articleNumber != null && !articleNumber.isEmpty()) {
            ref.append(" ").append(articleNumber);
        }
        if (sectionTitle != null && !sectionTitle.isEmpty()) {
            ref.append(" (").append(sectionTitle).append(")");
        }
        if (pageNumber != null) {
            ref.append(" [p.").append(pageNumber).append("]");
        }

        return ref.toString();
    }
}
