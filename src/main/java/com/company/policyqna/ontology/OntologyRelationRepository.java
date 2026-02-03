package com.company.policyqna.ontology;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OntologyRelationRepository extends JpaRepository<OntologyRelation, UUID> {

    List<OntologyRelation> findBySourceConceptId(UUID conceptId);

    List<OntologyRelation> findByTargetConceptId(UUID conceptId);

    List<OntologyRelation> findByRelationType(OntologyRelation.RelationType type);

    @Query("""
        SELECT r FROM OntologyRelation r
        WHERE r.sourceConcept.id = :conceptId OR r.targetConcept.id = :conceptId
    """)
    List<OntologyRelation> findAllRelationsForConcept(UUID conceptId);

    @Query("""
        SELECT r FROM OntologyRelation r
        WHERE r.relationType = :type
        AND (r.sourceConcept.id = :conceptId OR r.targetConcept.id = :conceptId)
    """)
    List<OntologyRelation> findRelationsForConceptByType(UUID conceptId, OntologyRelation.RelationType type);

    @Query("""
        SELECT r FROM OntologyRelation r
        WHERE r.relationType = 'REFERENCES'
        AND r.sourceConcept.id = :conceptId
    """)
    List<OntologyRelation> findReferencesFrom(UUID conceptId);

    @Query("""
        SELECT r FROM OntologyRelation r
        WHERE r.relationType = 'DEFINED_IN'
        AND r.sourceConcept.name = :termName
    """)
    List<OntologyRelation> findDefinitionLocation(String termName);
}
