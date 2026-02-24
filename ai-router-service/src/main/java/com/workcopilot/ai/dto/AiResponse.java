package com.workcopilot.ai.dto;

public record AiResponse(
        String result,
        String model,
        long processingTimeMs
) {
}
