package com.workcopilot.ai.controller;

import com.workcopilot.ai.dto.EmbeddingRequest;
import com.workcopilot.ai.dto.EmbeddingResponse;
import com.workcopilot.ai.service.EmbeddingService;
import com.workcopilot.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/internal/ai")
@RequiredArgsConstructor
public class InternalAiController {

    private final EmbeddingService embeddingService;

    /**
     * 문서 임베딩을 저장한다 (내부 서비스 간 호출용).
     */
    @PostMapping("/embedding")
    public ApiResponse<EmbeddingResponse> storeEmbedding(@RequestBody EmbeddingRequest request) {
        log.info("임베딩 저장 요청: documentId={}, documentType={}",
                request.documentId(), request.documentType());
        EmbeddingResponse response = embeddingService.embed(request);
        return ApiResponse.ok(response);
    }
}
