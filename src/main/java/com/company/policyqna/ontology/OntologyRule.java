package com.company.policyqna.ontology;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 온톨로지 규칙(Rule) 엔티티
 * - 비즈니스 규칙, 제약조건 등을 표현
 */
@Entity
@Table(name = "ontology_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OntologyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String condition;

    @Column(columnDefinition = "TEXT")
    private String consequence;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private Map<String, Object> parameters;

    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    @Column(name = "source_reference")
    private String sourceReference;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RuleType {
        REDIRECT("검색 리다이렉트", "특정 키워드 -> 특정 문서/조항으로 연결"),
        CONSTRAINT("제약조건", "값 범위, 필수 조건 등"),
        INFERENCE("추론규칙", "A이면 B도 검색"),
        SYNONYM("동의어 확장", "검색어 확장"),
        HIERARCHY("계층 확장", "상위/하위 개념 포함 검색");

        private final String koreanName;
        private final String description;

        RuleType(String koreanName, String description) {
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
