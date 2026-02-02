package com.company.policyqna.ontology;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OntologyRuleRepository extends JpaRepository<OntologyRule, Long> {

    List<OntologyRule> findByRuleType(OntologyRule.RuleType type);

    List<OntologyRule> findByIsActiveTrue();

    @Query("SELECT r FROM OntologyRule r WHERE r.isActive = true ORDER BY r.priority DESC")
    List<OntologyRule> findActiveRulesOrderByPriority();

    @Query("SELECT r FROM OntologyRule r WHERE r.ruleType = 'REDIRECT' AND r.condition LIKE %:keyword%")
    List<OntologyRule> findRedirectRulesForKeyword(String keyword);

    @Query("SELECT r FROM OntologyRule r WHERE r.ruleType = 'SYNONYM' AND r.isActive = true")
    List<OntologyRule> findActiveSynonymRules();
}
