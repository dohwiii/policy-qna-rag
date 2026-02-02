package com.company.policyqna.document;

import com.company.policyqna.domain.DocumentChunk;
import com.company.policyqna.domain.PolicyDocument;
import com.company.policyqna.domain.PolicyDocument.DocumentType;
import com.company.policyqna.vector.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 문서 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentParser documentParser;
    private final VectorStoreService vectorStoreService;

    @Value("${document.upload-path:./uploads}")
    private String uploadPath;

    /**
     * 문서 업로드 및 인덱싱
     */
    @Transactional
    public PolicyDocument uploadAndIndex(
            MultipartFile file,
            String title,
            String documentCode,
            DocumentType documentType,
            String department,
            Map<String, Object> metadata) throws IOException {

        // 1. 파일 저장
        Path savedPath = saveFile(file);
        log.info("File saved: {}", savedPath);

        // 2. 문서 엔티티 생성
        PolicyDocument document = PolicyDocument.builder()
            .title(title)
            .documentCode(documentCode)
            .documentType(documentType)
            .department(department)
            .filePath(savedPath.toString())
            .fileName(file.getOriginalFilename())
            .mimeType(file.getContentType())
            .metadata(metadata)
            .build();

        document = documentRepository.save(document);

        // 3. 문서 파싱 및 청킹
        DocumentParser.ParsedDocument parsed = documentParser.parseFile(savedPath);
        List<DocumentChunk> chunks = documentParser.createChunks(document, parsed.getContent());

        // 4. 청크 저장
        final PolicyDocument finalDocument = document;
        chunks.forEach(chunk -> chunk.setDocument(finalDocument));
        chunkRepository.saveAll(chunks);

        // 5. 벡터 스토어에 인덱싱
        vectorStoreService.indexChunks(chunks);

        // 6. 인덱싱 완료 표시
        document.setIndexed(true);
        document.setChunks(chunks);

        log.info("Document indexed: {} ({} chunks)", title, chunks.size());
        return documentRepository.save(document);
    }

    /**
     * 문서 재인덱싱
     */
    @Transactional
    public void reindex(Long documentId) throws IOException {
        PolicyDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // 기존 청크 삭제
        vectorStoreService.deleteByDocumentId(documentId);
        chunkRepository.deleteByDocumentId(documentId);

        // 재파싱 및 인덱싱
        Path filePath = Paths.get(document.getFilePath());
        DocumentParser.ParsedDocument parsed = documentParser.parseFile(filePath);
        List<DocumentChunk> chunks = documentParser.createChunks(document, parsed.getContent());

        final PolicyDocument finalDocument = document;
        chunks.forEach(chunk -> chunk.setDocument(finalDocument));
        chunkRepository.saveAll(chunks);
        vectorStoreService.indexChunks(chunks);

        document.setIndexed(true);
        documentRepository.save(document);

        log.info("Document re-indexed: {}", document.getTitle());
    }

    /**
     * 문서 검색
     */
    public List<PolicyDocument> search(String keyword, DocumentType type, String department) {
        if (type != null && department != null) {
            return documentRepository.findByDocumentTypeAndDepartmentContaining(type, department);
        } else if (type != null) {
            return documentRepository.findByDocumentType(type);
        } else if (department != null) {
            return documentRepository.findByDepartmentContaining(department);
        }
        return documentRepository.findByTitleContainingIgnoreCase(keyword);
    }

    /**
     * 문서 삭제
     */
    @Transactional
    public void delete(Long documentId) {
        PolicyDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // 벡터 스토어에서 삭제
        vectorStoreService.deleteByDocumentId(documentId);

        // 파일 삭제
        try {
            Files.deleteIfExists(Paths.get(document.getFilePath()));
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", document.getFilePath());
        }

        // DB에서 삭제
        documentRepository.delete(document);
        log.info("Document deleted: {}", document.getTitle());
    }

    private Path saveFile(MultipartFile file) throws IOException {
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path targetPath = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return targetPath;
    }
}
