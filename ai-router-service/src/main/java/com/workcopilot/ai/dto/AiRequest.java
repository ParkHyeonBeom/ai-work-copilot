package com.workcopilot.ai.dto;

import java.util.Map;

public record AiRequest(
        String taskType,
        String content,
        Map<String, Object> metadata
) {
}
