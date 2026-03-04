package com.workcopilot.ai.dto;

import java.util.List;

public record AgentChatResponse(
        Long conversationId,
        String reply,
        String model,
        long processingTimeMs,
        List<String> contextSources
) {
}
