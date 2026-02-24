package com.workcopilot.briefing.dto;

import java.util.List;

public record BriefingAiResponse(
        String summary,
        String fullContent,
        List<String> keyPoints,
        List<String> actionItems
) {
}
