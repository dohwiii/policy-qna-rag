package com.company.policyqna.document;

import com.company.policyqna.domain.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentId(Long documentId);

    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(Long documentId);

    @Query("SELECT c FROM DocumentChunk c WHERE c.vectorId IN :vectorIds")
    List<DocumentChunk> findByVectorIdIn(List<String> vectorIds);

    @Modifying
    @Query("DELETE FROM DocumentChunk c WHERE c.document.id = :documentId")
    void deleteByDocumentId(Long documentId);

    @Query("SELECT c FROM DocumentChunk c WHERE c.content LIKE %:keyword%")
    List<DocumentChunk> searchByKeyword(String keyword);

    @Query("SELECT c FROM DocumentChunk c WHERE c.articleNumber = :articleNumber")
    List<DocumentChunk> findByArticleNumber(String articleNumber);
}
