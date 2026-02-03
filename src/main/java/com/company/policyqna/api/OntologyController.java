package com.company.policyqna.api;

import com.company.policyqna.ontology.*;
import com.company.policyqna.ontology.OntologyConcept.ConceptType;
import com.company.policyqna.ontology.OntologyRelation.RelationType;
import com.company.policyqna.ontology.OntologyService.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 온톨로지 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/ontology")
@RequiredArgsConstructor
@Slf4j
public class OntologyController {

    private final OntologyService ontologyService;
    private final OntologyRepository conceptRepository;
    private final OntologyRelationRepository relationRepository;
    private final OntologyRuleRepository ruleRepository;

    // ==================== 개념 관리 ====================

    /**
     * 개념 생성
     */
    @PostMapping("/concepts")
    public ResponseEntity<OntologyConcept> createConcept(@Valid @RequestBody ConceptRequest request) {
        OntologyConcept concept = ontologyService.createConcept(
            request.name(),
            ConceptType.valueOf(request.conceptType()),
            request.definition(),
            request.synonyms(),
            request.abbreviations()
        );
        return ResponseEntity.ok(concept);
    }

    /**
     * 개념 목록 조회
     */
    @GetMapping("/concepts")
    public ResponseEntity<List<OntologyConcept>> listConcepts(
            @RequestParam(required = false) String type) {

        List<OntologyConcept> concepts;
        if (type != null) {
            concepts = conceptRepository.findByConceptType(ConceptType.valueOf(type));
        } else {
            concepts = conceptRepository.findAll();
        }
        return ResponseEntity.ok(concepts);
    }

    /**
     * 개념 검색
     */
    @GetMapping("/concepts/search")
    public ResponseEntity<List<OntologyConcept>> searchConcepts(@RequestParam String keyword) {
        return ResponseEntity.ok(conceptRepository.searchByKeyword(keyword));
    }

    /**
     * 개념 상세 조회 (관계 포함)
     */
    @GetMapping("/concepts/{id}")
    public ResponseEntity<OntologyConcept> getConcept(@PathVariable UUID id) {
        return conceptRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 개념 그래프 조회
     */
    @GetMapping("/concepts/{id}/graph")
    public ResponseEntity<ConceptGraph> getConceptGraph(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "2") int depth) {

        return ResponseEntity.ok(ontologyService.getConceptGraph(id, depth));
    }

    // ==================== 관계 관리 ====================

    /**
     * 관계 생성
     */
    @PostMapping("/relations")
    public ResponseEntity<OntologyRelation> createRelation(@Valid @RequestBody RelationRequest request) {
        OntologyRelation relation = ontologyService.createRelation(
            request.sourceConceptId(),
            request.targetConceptId(),
            RelationType.valueOf(request.relationType()),
            request.description()
        );
        return ResponseEntity.ok(relation);
    }

    /**
     * 개념의 관계 조회
     */
    @GetMapping("/concepts/{id}/relations")
    public ResponseEntity<List<OntologyRelation>> getConceptRelations(@PathVariable UUID id) {
        return ResponseEntity.ok(relationRepository.findAllRelationsForConcept(id));
    }

    // ==================== 규칙 관리 ====================

    /**
     * 규칙 생성
     */
    @PostMapping("/rules")
    public ResponseEntity<OntologyRule> createRule(@Valid @RequestBody RuleRequest request) {
        OntologyRule rule = OntologyRule.builder()
            .name(request.name())
            .ruleType(OntologyRule.RuleType.valueOf(request.ruleType()))
            .condition(request.condition())
            .consequence(request.consequence())
            .description(request.description())
            .priority(request.priority() != null ? request.priority() : 0)
            .isActive(true)
            .build();

        return ResponseEntity.ok(ruleRepository.save(rule));
    }

    /**
     * 규칙 목록 조회
     */
    @GetMapping("/rules")
    public ResponseEntity<List<OntologyRule>> listRules(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "true") boolean activeOnly) {

        List<OntologyRule> rules;
        if (activeOnly) {
            rules = ruleRepository.findActiveRulesOrderByPriority();
        } else if (type != null) {
            rules = ruleRepository.findByRuleType(OntologyRule.RuleType.valueOf(type));
        } else {
            rules = ruleRepository.findAll();
        }
        return ResponseEntity.ok(rules);
    }

    /**
     * 규칙 활성화/비활성화
     */
    @PatchMapping("/rules/{id}/toggle")
    public ResponseEntity<OntologyRule> toggleRule(@PathVariable Long id) {
        return ruleRepository.findById(id)
            .map(rule -> {
                rule.setIsActive(!rule.getIsActive());
                return ResponseEntity.ok(ruleRepository.save(rule));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // ==================== 용어 정의 ====================

    /**
     * 용어 정의 조회
     */
    @GetMapping("/terms/{term}")
    public ResponseEntity<TermDefinition> getTermDefinition(@PathVariable String term) {
        return ontologyService.getTermDefinition(term)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 쿼리 확장 테스트
     */
    @GetMapping("/query/expand")
    public ResponseEntity<QueryExpansion> expandQuery(@RequestParam String query) {
        return ResponseEntity.ok(ontologyService.expandQuery(query));
    }

    // ==================== Request DTOs ====================

    public record ConceptRequest(
        @NotBlank String name,
        @NotBlank String conceptType,
        String definition,
        List<String> synonyms,
        List<String> abbreviations
    ) {}

    public record RelationRequest(
        UUID sourceConceptId,
        UUID targetConceptId,
        @NotBlank String relationType,
        String description
    ) {}

    public record RuleRequest(
        @NotBlank String name,
        @NotBlank String ruleType,
        @NotBlank String condition,
        String consequence,
        String description,
        Integer priority
    ) {}
}
