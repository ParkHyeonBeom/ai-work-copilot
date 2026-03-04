package com.workcopilot.ai.dto;

import java.util.List;

public record ConversationDetailDto(
        Long id,
        String title,
        List<MessageDto> messages
) {
}
