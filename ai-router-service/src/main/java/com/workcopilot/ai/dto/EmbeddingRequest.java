package com.workcopilot.ai.dto;

public record EmbeddingRequest(
        String text,
        String documentId,
        String documentType
) {
}
