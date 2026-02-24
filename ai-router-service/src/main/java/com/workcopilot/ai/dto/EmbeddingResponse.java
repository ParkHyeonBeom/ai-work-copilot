package com.workcopilot.ai.dto;

public record EmbeddingResponse(
        String documentId,
        boolean success
) {
}
