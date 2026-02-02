package com.company.policyqna.ontology;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OntologyRelationRepository extends JpaRepository<OntologyRelation, Long> {

    List<OntologyRelation> findBySourceConceptId(Long conceptId);

    List<OntologyRelation> findByTargetConceptId(Long conceptId);

    List<OntologyRelation> findByRelationType(OntologyRelation.RelationType type);

    @Query("""
        SELECT r FROM OntologyRelation r
        WHERE r.sourceConcept.id = :conceptId OR r.targetConcept.id = :conceptId
    """)
    List<OntologyRelation> findAllRelationsForConcept(Long conceptId);

    @Query("""
        SELECT r FROM OntologyRelation r
        WHERE r.relationType = :type
        AND (r.sourceConcept.id = :conceptId OR r.targetConcept.id = :conceptId)
    """)
    List<OntologyRelation> findRelationsForConceptByType(Long conceptId, OntologyRelation.RelationType type);

    @Query("""
        SELECT r FROM OntologyRelation r
        WHERE r.relationType = 'REFERENCES'
        AND r.sourceConcept.id = :conceptId
    """)
    List<OntologyRelation> findReferencesFrom(Long conceptId);

    @Query("""
        SELECT r FROM OntologyRelation r
        WHERE r.relationType = 'DEFINED_IN'
        AND r.sourceConcept.name = :termName
    """)
    List<OntologyRelation> findDefinitionLocation(String termName);
}
