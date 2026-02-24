package com.workcopilot.ai.service;

import com.workcopilot.ai.dto.EmbeddingRequest;
import com.workcopilot.ai.dto.EmbeddingResponse;
import com.workcopilot.ai.service.vector.VectorStoreService;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final VectorStoreService vectorStoreService;

    /**
     * 문서를 임베딩하여 벡터 스토어에 저장한다.
     */
    public EmbeddingResponse embed(EmbeddingRequest request) {
        try {
            Map<String, String> metadata = Map.of(
                    "documentType", request.documentType(),
                    "documentId", request.documentId()
            );

            vectorStoreService.store(request.documentId(), request.text(), metadata);

            log.info("임베딩 저장 완료: documentId={}, documentType={}",
                    request.documentId(), request.documentType());
            return new EmbeddingResponse(request.documentId(), true);
        } catch (Exception e) {
            log.error("임베딩 저장 실패: documentId={}, error={}",
                    request.documentId(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.EMBEDDING_ERROR,
                    "임베딩 저장 실패: " + e.getMessage());
        }
    }
}
