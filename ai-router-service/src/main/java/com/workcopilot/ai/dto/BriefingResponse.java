package com.workcopilot.ai.dto;

import java.util.List;

public record BriefingResponse(
        String summary,
        String fullContent,
        List<String> keyPoints,
        List<String> actionItems
) {
}
