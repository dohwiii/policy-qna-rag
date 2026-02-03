package com.company.policyqna.ontology;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 온톨로지 관리 서비스
 * - 개념, 관계, 규칙 관리
 * - 검색 쿼리 확장 및 최적화
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OntologyService {

    private final OntologyRepository conceptRepository;
    private final OntologyRelationRepository relationRepository;
    private final OntologyRuleRepository ruleRepository;

    // ==================== 개념 관리 ====================

    @Transactional
    public OntologyConcept createConcept(
            String name,
            OntologyConcept.ConceptType type,
            String definition,
            List<String> synonyms,
            List<String> abbreviations) {

        OntologyConcept concept = OntologyConcept.builder()
            .name(name)
            .conceptType(type)
            .definition(definition)
            .synonyms(synonyms != null ? synonyms : new ArrayList<>())
            .abbreviations(abbreviations != null ? abbreviations : new ArrayList<>())
            .build();

        return conceptRepository.save(concept);
    }

    @Transactional
    public OntologyRelation createRelation(
            UUID sourceId,
            UUID targetId,
            OntologyRelation.RelationType type,
            String description) {

        OntologyConcept source = conceptRepository.findById(sourceId)
            .orElseThrow(() -> new IllegalArgumentException("Source concept not found"));
        OntologyConcept target = conceptRepository.findById(targetId)
            .orElseThrow(() -> new IllegalArgumentException("Target concept not found"));

        OntologyRelation relation = OntologyRelation.builder()
            .sourceConcept(source)
            .targetConcept(target)
            .relationType(type)
            .description(description)
            .build();

        return relationRepository.save(relation);
    }

    // ==================== 검색 확장 ====================

    /**
     * 검색어 확장 - 동의어, 약어, 관련 개념 포함
     */
    public QueryExpansion expandQuery(String query) {
        Set<String> expandedTerms = new HashSet<>();
        expandedTerms.add(query);

        List<OntologyConcept> matchedConcepts = new ArrayList<>();
        Map<String, Double> termWeights = new HashMap<>();
        termWeights.put(query, 1.0);

        // 1. 정확히 일치하는 개념 찾기
        conceptRepository.findByName(query).ifPresent(concept -> {
            matchedConcepts.add(concept);
            addConceptTerms(concept, expandedTerms, termWeights);
        });

        // 2. 동의어/약어로 검색
        conceptRepository.findBySynonym(query).forEach(concept -> {
            matchedConcepts.add(concept);
            expandedTerms.add(concept.getName());
            termWeights.put(concept.getName(), 0.9);
        });

        conceptRepository.findByAbbreviation(query).forEach(concept -> {
            matchedConcepts.add(concept);
            expandedTerms.add(concept.getName());
            termWeights.put(concept.getName(), 0.9);
        });

        // 3. 규칙 기반 확장
        applyExpansionRules(query, expandedTerms, termWeights);

        // 4. 관련 개념 추가 (가중치 낮춤)
        for (OntologyConcept concept : matchedConcepts) {
            addRelatedConcepts(concept, expandedTerms, termWeights);
        }

        log.debug("Query '{}' expanded to: {}", query, expandedTerms);

        return QueryExpansion.builder()
            .originalQuery(query)
            .expandedTerms(new ArrayList<>(expandedTerms))
            .termWeights(termWeights)
            .matchedConcepts(matchedConcepts)
            .build();
    }

    /**
     * 개념의 용어들 추가
     */
    private void addConceptTerms(OntologyConcept concept, Set<String> terms, Map<String, Double> weights) {
        // 동의어 추가
        for (String synonym : concept.getSynonyms()) {
            terms.add(synonym);
            weights.putIfAbsent(synonym, 0.8);
        }

        // 약어 추가
        for (String abbr : concept.getAbbreviations()) {
            terms.add(abbr);
            weights.putIfAbsent(abbr, 0.7);
        }
    }

    /**
     * 관련 개념 추가
     */
    private void addRelatedConcepts(OntologyConcept concept, Set<String> terms, Map<String, Double> weights) {
        List<OntologyRelation> relations = relationRepository.findBySourceConceptId(concept.getId());

        for (OntologyRelation relation : relations) {
            OntologyConcept related = relation.getTargetConcept();
            double baseWeight = relation.getWeight() != null ? relation.getWeight() : 0.5;

            // 관계 타입에 따른 가중치 조정
            double weight = switch (relation.getRelationType()) {
                case IS_A, PART_OF -> baseWeight * 0.8;
                case REFERENCES, RELATED_TO -> baseWeight * 0.6;
                default -> baseWeight * 0.5;
            };

            terms.add(related.getName());
            weights.putIfAbsent(related.getName(), weight);
        }
    }

    /**
     * 규칙 기반 확장
     */
    private void applyExpansionRules(String query, Set<String> terms, Map<String, Double> weights) {
        List<OntologyRule> rules = ruleRepository.findActiveSynonymRules();

        for (OntologyRule rule : rules) {
            if (query.toLowerCase().contains(rule.getCondition().toLowerCase())) {
                String consequence = rule.getConsequence();
                if (consequence != null && !consequence.isEmpty()) {
                    terms.add(consequence);
                    weights.putIfAbsent(consequence, 0.7);
                }
            }
        }
    }

    // ==================== 리다이렉트 규칙 ====================

    /**
     * 검색 리다이렉트 - 특정 키워드에 대해 직접 문서/조항 연결
     */
    public Optional<RedirectResult> checkRedirect(String query) {
        List<OntologyRule> redirectRules = ruleRepository.findRedirectRulesForKeyword(query);

        for (OntologyRule rule : redirectRules) {
            if (matchesCondition(query, rule.getCondition())) {
                return Optional.of(RedirectResult.builder()
                    .targetReference(rule.getConsequence())
                    .description(rule.getDescription())
                    .ruleName(rule.getName())
                    .build());
            }
        }

        return Optional.empty();
    }

    private boolean matchesCondition(String query, String condition) {
        // 간단한 키워드 매칭 (확장 가능)
        return query.toLowerCase().contains(condition.toLowerCase());
    }

    // ==================== 개념 그래프 탐색 ====================

    /**
     * 개념 관계 그래프 조회
     */
    public ConceptGraph getConceptGraph(UUID conceptId, int depth) {
        OntologyConcept rootConcept = conceptRepository.findById(conceptId)
            .orElseThrow(() -> new IllegalArgumentException("Concept not found"));

        Set<UUID> visited = new HashSet<>();
        List<OntologyConcept> concepts = new ArrayList<>();
        List<OntologyRelation> relations = new ArrayList<>();

        traverseGraph(rootConcept, depth, visited, concepts, relations);

        return ConceptGraph.builder()
            .rootConcept(rootConcept)
            .concepts(concepts)
            .relations(relations)
            .build();
    }

    private void traverseGraph(
            OntologyConcept concept,
            int depth,
            Set<UUID> visited,
            List<OntologyConcept> concepts,
            List<OntologyRelation> relations) {

        if (depth < 0 || visited.contains(concept.getId())) {
            return;
        }

        visited.add(concept.getId());
        concepts.add(concept);

        List<OntologyRelation> outgoing = relationRepository.findBySourceConceptId(concept.getId());
        for (OntologyRelation rel : outgoing) {
            relations.add(rel);
            traverseGraph(rel.getTargetConcept(), depth - 1, visited, concepts, relations);
        }
    }

    // ==================== 용어 정의 조회 ====================

    /**
     * 용어 정의 및 출처 조회
     */
    public Optional<TermDefinition> getTermDefinition(String term) {
        return conceptRepository.findByName(term)
            .map(concept -> {
                List<OntologyRelation> definitions =
                    relationRepository.findDefinitionLocation(term);

                return TermDefinition.builder()
                    .term(term)
                    .definition(concept.getDefinition())
                    .conceptType(concept.getConceptType())
                    .synonyms(concept.getSynonyms())
                    .sourceReferences(definitions.stream()
                        .map(r -> r.getTargetConcept().getName())
                        .collect(Collectors.toList()))
                    .build();
            });
    }

    // ==================== DTO 클래스들 ====================

    @lombok.Builder
    @lombok.Getter
    public static class QueryExpansion {
        private String originalQuery;
        private List<String> expandedTerms;
        private Map<String, Double> termWeights;
        private List<OntologyConcept> matchedConcepts;
    }

    @lombok.Builder
    @lombok.Getter
    public static class RedirectResult {
        private String targetReference;
        private String description;
        private String ruleName;
    }

    @lombok.Builder
    @lombok.Getter
    public static class ConceptGraph {
        private OntologyConcept rootConcept;
        private List<OntologyConcept> concepts;
        private List<OntologyRelation> relations;
    }

    @lombok.Builder
    @lombok.Getter
    public static class TermDefinition {
        private String term;
        private String definition;
        private OntologyConcept.ConceptType conceptType;
        private List<String> synonyms;
        private List<String> sourceReferences;
    }
}
