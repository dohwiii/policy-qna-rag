package com.company.policyqna.api;

import com.company.policyqna.document.DocumentRepository;
import com.company.policyqna.document.DocumentService;
import com.company.policyqna.domain.PolicyDocument;
import com.company.policyqna.domain.PolicyDocument.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 문서 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    /**
     * 문서 업로드 및 인덱싱
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "documentCode", required = false) String documentCode,
            @RequestParam(value = "documentType", defaultValue = "MANUAL") String documentType,
            @RequestParam(value = "department", required = false) String department) {

        try {
            PolicyDocument document = documentService.uploadAndIndex(
                file,
                title,
                documentCode,
                DocumentType.valueOf(documentType),
                department,
                Map.of("originalFileName", file.getOriginalFilename())
            );

            return ResponseEntity.ok(toResponse(document));

        } catch (IOException e) {
            log.error("Failed to upload document", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 문서 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> listDocuments(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String department) {

        List<PolicyDocument> documents;

        if (type != null && department != null) {
            documents = documentRepository.findByDocumentTypeAndDepartmentContaining(
                DocumentType.valueOf(type), department);
        } else if (type != null) {
            documents = documentRepository.findByDocumentType(DocumentType.valueOf(type));
        } else if (department != null) {
            documents = documentRepository.findByDepartmentContaining(department);
        } else {
            documents = documentRepository.findAll();
        }

        return ResponseEntity.ok(documents.stream().map(this::toResponse).toList());
    }

    /**
     * 문서 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
            .map(doc -> ResponseEntity.ok(toResponse(doc)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 문서 재인덱싱
     */
    @PostMapping("/{id}/reindex")
    public ResponseEntity<Void> reindexDocument(@PathVariable Long id) {
        try {
            documentService.reindex(id);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Failed to reindex document", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 문서 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 인덱싱 통계
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalDocuments = documentRepository.count();
        long indexedDocuments = documentRepository.countIndexedDocuments();

        return ResponseEntity.ok(Map.of(
            "totalDocuments", totalDocuments,
            "indexedDocuments", indexedDocuments,
            "pendingDocuments", totalDocuments - indexedDocuments
        ));
    }

    private DocumentResponse toResponse(PolicyDocument doc) {
        return new DocumentResponse(
            doc.getId(),
            doc.getTitle(),
            doc.getDocumentCode(),
            doc.getDocumentType().name(),
            doc.getDocumentType().getKoreanName(),
            doc.getDepartment(),
            doc.getFileName(),
            doc.isIndexed(),
            doc.getChunks() != null ? doc.getChunks().size() : 0,
            doc.getCreatedAt(),
            doc.getUpdatedAt()
        );
    }

    public record DocumentResponse(
        Long id,
        String title,
        String documentCode,
        String documentType,
        String documentTypeName,
        String department,
        String fileName,
        boolean indexed,
        int chunkCount,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt
    ) {}
}
