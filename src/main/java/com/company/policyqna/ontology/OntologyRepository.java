package com.company.policyqna.ontology;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OntologyRepository extends JpaRepository<OntologyConcept, UUID> {

    Optional<OntologyConcept> findByName(String name);

    List<OntologyConcept> findByConceptType(OntologyConcept.ConceptType type);

    @Query("SELECT c FROM OntologyConcept c WHERE c.name LIKE %:keyword% OR :keyword MEMBER OF c.synonyms")
    List<OntologyConcept> searchByKeyword(String keyword);

    @Query("SELECT c FROM OntologyConcept c WHERE :keyword MEMBER OF c.synonyms")
    List<OntologyConcept> findBySynonym(String keyword);

    @Query("SELECT c FROM OntologyConcept c WHERE :abbr MEMBER OF c.abbreviations")
    List<OntologyConcept> findByAbbreviation(String abbr);

    @Query("SELECT c FROM OntologyConcept c WHERE c.sourceDocumentId = :documentId")
    List<OntologyConcept> findBySourceDocument(Long documentId);

    @Query("""
        SELECT DISTINCT c FROM OntologyConcept c
        LEFT JOIN FETCH c.outgoingRelations r
        WHERE c.name LIKE %:keyword%
        OR :keyword MEMBER OF c.synonyms
        OR :keyword MEMBER OF c.abbreviations
    """)
    List<OntologyConcept> findWithRelationsByKeyword(String keyword);
}
