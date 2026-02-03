package com.company.policyqna.ontology;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 온톨로지 개념(Concept) 엔티티
 * - 용어, 부서, 직급, 프로세스 등의 개념을 표현
 */
@Entity
@Table(name = "ontology_concepts", indexes = {
    @Index(name = "idx_concept_name", columnList = "name"),
    @Index(name = "idx_concept_type", columnList = "concept_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OntologyConcept {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_en")
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "concept_type", nullable = false)
    private ConceptType conceptType;

    @Column(columnDefinition = "TEXT")
    private String definition;

    @ElementCollection
    @CollectionTable(name = "concept_synonyms", joinColumns = @JoinColumn(name = "concept_id"))
    @Column(name = "synonym")
    @Builder.Default
    private List<String> synonyms = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "concept_abbreviations", joinColumns = @JoinColumn(name = "concept_id"))
    @Column(name = "abbreviation")
    @Builder.Default
    private List<String> abbreviations = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "jsonb")
    private Map<String, Object> properties;

    @OneToMany(mappedBy = "sourceConcept", cascade = CascadeType.ALL)
    @Builder.Default
    private List<OntologyRelation> outgoingRelations = new ArrayList<>();

    @OneToMany(mappedBy = "targetConcept", cascade = CascadeType.ALL)
    @Builder.Default
    private List<OntologyRelation> incomingRelations = new ArrayList<>();

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

    public enum ConceptType {
        TERM("용어"),
        DEPARTMENT("부서"),
        ROLE("직급/역할"),
        PROCESS("프로세스"),
        POLICY("정책"),
        REGULATION("규정"),
        ARTICLE("조항"),
        FORM("양식"),
        SYSTEM("시스템");

        private final String koreanName;

        ConceptType(String koreanName) {
            this.koreanName = koreanName;
        }

        public String getKoreanName() {
            return koreanName;
        }
    }
}
