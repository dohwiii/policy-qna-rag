package com.company.policyqna.api;

import com.company.policyqna.rag.RagPipelineService;
import com.company.policyqna.rag.RagPipelineService.QnaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Q&A API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/qna")
@RequiredArgsConstructor
@Slf4j
public class QnaController {

    private final RagPipelineService ragPipelineService;

    /**
     * 질문 응답 API
     */
    @PostMapping("/ask")
    public ResponseEntity<QnaResponse> ask(@Valid @RequestBody QuestionRequest request) {
        log.info("Received question: {}", request.question());

        QnaResponse response = ragPipelineService.processQuestion(request.question());

        return ResponseEntity.ok(response);
    }

    /**
     * 스트리밍 응답 (SSE) - 추후 구현
     */
    @PostMapping("/ask/stream")
    public ResponseEntity<Void> askStream(@Valid @RequestBody QuestionRequest request) {
        // TODO: Server-Sent Events 구현
        return ResponseEntity.status(501).build();
    }

    public record QuestionRequest(
        @NotBlank(message = "질문을 입력해주세요")
        String question,

        String department,  // 특정 부서 문서로 제한 (선택)

        String documentType  // 특정 문서 유형으로 제한 (선택)
    ) {}
}
