package com.workcopilot.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentChatRequest(
        Long conversationId,
        @NotBlank(message = "메시지를 입력해주세요.")
        String message
) {
}
