package com.company.policyqna.api;

import com.company.policyqna.rag.RagPipelineService;
import com.company.policyqna.rag.RagPipelineService.QnaResponse;
import com.company.policyqna.rag.RagPipelineService.RagContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

@Tag(name = "Q&A API", description = "사내 정책/매뉴얼 질의응답 API")
@RestController
@RequestMapping("/api/v1/qna")
@RequiredArgsConstructor
@Slf4j
public class QnaController {

    private final RagPipelineService ragPipelineService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "동기 Q&A", description = "RAG 파이프라인 전체 실행 후 완성된 답변을 한 번에 반환")
    @PostMapping("/ask")
    public ResponseEntity<QnaResponse> ask(@Valid @RequestBody QuestionRequest request) {
        log.info("Received question: {}", request.question());
        QnaResponse response = ragPipelineService.processQuestion(request.question());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "스트리밍 Q&A (SSE)", description = "Server-Sent Events로 토큰 단위 실시간 스트리밍. 마지막에 출처 정보 포함")
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@Valid @RequestBody QuestionRequest request) {
        log.info("Received stream question: {}", request.question());

        // Ollama CPU 응답 속도를 고려해 3분 타임아웃
        SseEmitter emitter = new SseEmitter(180_000L);
        var executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            try {
                // 1~6단계: 온톨로지 + 벡터 검색 (동기)
                RagContext ctx = ragPipelineService.buildRagContext(request.question());

                if (ctx.noResults()) {
                    String noResultMsg = ctx.redirectInfo() != null
                        ? "'" + ctx.redirectInfo().getTargetReference() + "' 참조 문서를 찾을 수 없습니다."
                        : "질문과 관련된 문서를 찾을 수 없습니다. 다른 키워드로 검색해 주세요.";
                    emitter.send(SseEmitter.event().name("token")
                        .data(objectMapper.writeValueAsString(Map.of("type", "token", "text", noResultMsg))));
                    emitter.send(SseEmitter.event().name("done")
                        .data(objectMapper.writeValueAsString(buildDonePayload(ctx))));
                    emitter.complete();
                    return;
                }

                // 7단계: LLM 스트리밍 (Flux)
                // blockLast()를 Executor 스레드에서 호출 → Reactor 스레드가 아니므로 안전
                ragPipelineService.streamAnswer(ctx)
                    .doOnNext(token -> {
                        try {
                            emitter.send(SseEmitter.event().name("token")
                                .data(objectMapper.writeValueAsString(
                                    Map.of("type", "token", "text", token))));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .doOnComplete(() -> {
                        try {
                            emitter.send(SseEmitter.event().name("done")
                                .data(objectMapper.writeValueAsString(buildDonePayload(ctx))));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .doOnError(emitter::completeWithError)
                    .blockLast();

            } catch (Exception e) {
                log.error("Stream error for question: {}", request.question(), e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                        .data(objectMapper.writeValueAsString(
                            Map.of("type", "error", "message", "처리 중 오류가 발생했습니다."))));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            } finally {
                executor.shutdown();
            }
        });

        return emitter;
    }

    private Map<String, Object> buildDonePayload(RagContext ctx) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "done");
        payload.put("sources", ctx.sources() != null ? ctx.sources() : java.util.Collections.emptyList());
        payload.put("relatedTerms", ctx.relatedTerms() != null ? ctx.relatedTerms() : java.util.Collections.emptyList());
        return payload;
    }

    public record QuestionRequest(
        @NotBlank(message = "질문을 입력해주세요")
        String question,

        String department,

        String documentType
    ) {}
}
