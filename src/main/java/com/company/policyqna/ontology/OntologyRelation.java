package com.company.policyqna.ontology;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 온톨로지 관계(Relation) 엔티티
 * - 개념 간의 관계를 표현
 */
@Entity
@Table(name = "ontology_relations", indexes = {
    @Index(name = "idx_relation_type", columnList = "relation_type"),
    @Index(name = "idx_source_concept", columnList = "source_concept_id"),
    @Index(name = "idx_target_concept", columnList = "target_concept_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OntologyRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_concept_id", nullable = false)
    private OntologyConcept sourceConcept;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_concept_id", nullable = false)
    private OntologyConcept targetConcept;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false)
    private RelationType relationType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "weight")
    @Builder.Default
    private Double weight = 1.0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "jsonb")
    private Map<String, Object> properties;

    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum RelationType {
        // 계층 관계
        IS_A("~의 일종", "상위 개념"),
        PART_OF("~의 일부", "포함 관계"),
        HAS_PART("~을 포함", "구성 요소"),

        // 참조 관계
        REFERENCES("참조", "다른 조항/문서 참조"),
        DEFINED_IN("정의됨", "용어 정의 위치"),
        APPLIES_TO("적용대상", "정책 적용 범위"),

        // 프로세스 관계
        PRECEDES("선행", "순서 관계"),
        FOLLOWS("후행", "순서 관계"),
        REQUIRES("필요", "의존 관계"),

        // 조직 관계
        BELONGS_TO("소속", "부서 소속"),
        REPORTS_TO("보고", "보고 체계"),
        APPROVES("승인권한", "결재 관계"),

        // 문서 관계
        SUPERSEDES("대체", "문서 버전"),
        RELATED_TO("관련", "일반 관계");

        private final String koreanName;
        private final String description;

        RelationType(String koreanName, String description) {
            this.koreanName = koreanName;
            this.description = description;
        }

        public String getKoreanName() {
            return koreanName;
        }

        public String getDescription() {
            return description;
        }
    }
}
