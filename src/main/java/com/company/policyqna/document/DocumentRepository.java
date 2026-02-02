package com.company.policyqna.document;

import com.company.policyqna.domain.PolicyDocument;
import com.company.policyqna.domain.PolicyDocument.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<PolicyDocument, Long> {

    Optional<PolicyDocument> findByDocumentCode(String documentCode);

    List<PolicyDocument> findByDocumentType(DocumentType type);

    List<PolicyDocument> findByDepartmentContaining(String department);

    List<PolicyDocument> findByDocumentTypeAndDepartmentContaining(DocumentType type, String department);

    List<PolicyDocument> findByTitleContainingIgnoreCase(String keyword);

    List<PolicyDocument> findByIndexedFalse();

    @Query("SELECT d FROM PolicyDocument d WHERE d.indexed = true ORDER BY d.updatedAt DESC")
    List<PolicyDocument> findAllIndexedDocuments();

    @Query("SELECT COUNT(d) FROM PolicyDocument d WHERE d.indexed = true")
    long countIndexedDocuments();
}
