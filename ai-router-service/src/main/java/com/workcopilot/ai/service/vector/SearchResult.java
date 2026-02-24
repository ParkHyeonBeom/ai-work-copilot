package com.workcopilot.ai.service.vector;

import java.util.Map;

public record SearchResult(
        String documentId,
        String content,
        double score,
        Map<String, String> metadata
) {
}
