package com.company.policyqna.document;

import com.company.policyqna.domain.DocumentChunk;
import com.company.policyqna.domain.PolicyDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 문서 파싱 서비스
 * - PDF, Word, HWP 등 다양한 문서 형식 지원
 * - 텍스트 추출 및 청킹
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentParser {

    private final Tika tika = new Tika();
    private final AutoDetectParser parser = new AutoDetectParser();

    @Value("${document.chunk-size:1000}")
    private int chunkSize;

    @Value("${document.chunk-overlap:200}")
    private int chunkOverlap;

    // 조항 번호 패턴 (제1조, 제1장, 1.1, 1.1.1 등)
    private static final Pattern ARTICLE_PATTERN = Pattern.compile(
        "^(제\\s*\\d+\\s*[조장절항]|\\d+\\.\\d+(\\.\\d+)*|[①②③④⑤⑥⑦⑧⑨⑩]|[가나다라마바사아자차카타파하]\\.)\\s*"
    );

    // 섹션 제목 패턴
    private static final Pattern SECTION_PATTERN = Pattern.compile(
        "^(제\\s*\\d+\\s*[장절]|[IVX]+\\.|\\d+\\.)\\s*(.+)$"
    );

    /**
     * 파일에서 텍스트 추출
     */
    public ParsedDocument parseFile(Path filePath) throws IOException {
        String mimeType = tika.detect(filePath);
        log.info("Parsing file: {} ({})", filePath.getFileName(), mimeType);

        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler(-1); // 무제한
        ParseContext context = new ParseContext();

        try (InputStream stream = Files.newInputStream(filePath)) {
            parser.parse(stream, handler, metadata, context);
        } catch (SAXException | TikaException e) {
            throw new IOException("Failed to parse document: " + e.getMessage(), e);
        }

        String content = handler.toString();
        Map<String, String> metadataMap = extractMetadata(metadata);

        return ParsedDocument.builder()
            .content(content)
            .mimeType(mimeType)
            .metadata(metadataMap)
            .build();
    }

    /**
     * 텍스트를 청크로 분할
     */
    public List<DocumentChunk> createChunks(PolicyDocument document, String content) {
        List<DocumentChunk> chunks = new ArrayList<>();

        // 문단 단위로 분리
        List<TextSegment> segments = splitIntoSegments(content);

        // 청크 생성
        StringBuilder currentChunk = new StringBuilder();
        String currentSection = null;
        String currentArticle = null;
        int chunkIndex = 0;
        int startOffset = 0;

        for (TextSegment segment : segments) {
            // 섹션/조항 정보 업데이트
            if (segment.getSection() != null) {
                currentSection = segment.getSection();
            }
            if (segment.getArticle() != null) {
                currentArticle = segment.getArticle();
            }

            String text = segment.getText();

            if (currentChunk.length() + text.length() > chunkSize) {
                // 현재 청크 저장
                if (currentChunk.length() > 0) {
                    chunks.add(createChunkEntity(
                        document, chunkIndex++, currentChunk.toString().trim(),
                        currentSection, currentArticle, startOffset
                    ));
                }

                // 오버랩 적용
                String overlap = getOverlapText(currentChunk.toString());
                currentChunk = new StringBuilder(overlap);
                startOffset = startOffset + chunkSize - chunkOverlap;
            }

            currentChunk.append(text).append("\n");
        }

        // 마지막 청크 저장
        if (currentChunk.length() > 0) {
            chunks.add(createChunkEntity(
                document, chunkIndex, currentChunk.toString().trim(),
                currentSection, currentArticle, startOffset
            ));
        }

        log.info("Created {} chunks from document: {}", chunks.size(), document.getTitle());
        return chunks;
    }

    /**
     * 텍스트를 세그먼트(문단/조항)로 분리
     */
    private List<TextSegment> splitIntoSegments(String content) {
        List<TextSegment> segments = new ArrayList<>();
        String[] lines = content.split("\n");

        String currentSection = null;
        String currentArticle = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 섹션 제목 확인
            Matcher sectionMatcher = SECTION_PATTERN.matcher(line);
            if (sectionMatcher.find()) {
                currentSection = sectionMatcher.group(2);
            }

            // 조항 번호 확인
            Matcher articleMatcher = ARTICLE_PATTERN.matcher(line);
            if (articleMatcher.find()) {
                currentArticle = articleMatcher.group(1).trim();
            }

            segments.add(TextSegment.builder()
                .text(line)
                .section(currentSection)
                .article(currentArticle)
                .build());
        }

        return segments;
    }

    /**
     * 오버랩 텍스트 추출
     */
    private String getOverlapText(String text) {
        if (text.length() <= chunkOverlap) {
            return text;
        }
        return text.substring(text.length() - chunkOverlap);
    }

    /**
     * 청크 엔티티 생성
     */
    private DocumentChunk createChunkEntity(
            PolicyDocument document,
            int index,
            String content,
            String section,
            String article,
            int startOffset) {

        return DocumentChunk.builder()
            .document(document)
            .chunkIndex(index)
            .content(content)
            .sectionTitle(section)
            .articleNumber(article)
            .startOffset(startOffset)
            .endOffset(startOffset + content.length())
            .metadata(Map.of(
                "wordCount", content.split("\\s+").length,
                "charCount", content.length()
            ))
            .build();
    }

    /**
     * Tika 메타데이터 추출
     */
    private Map<String, String> extractMetadata(Metadata metadata) {
        Map<String, String> result = new HashMap<>();
        for (String name : metadata.names()) {
            result.put(name, metadata.get(name));
        }
        return result;
    }

    @lombok.Builder
    @lombok.Getter
    public static class ParsedDocument {
        private String content;
        private String mimeType;
        private Map<String, String> metadata;
    }

    @lombok.Builder
    @lombok.Getter
    private static class TextSegment {
        private String text;
        private String section;
        private String article;
    }
}
