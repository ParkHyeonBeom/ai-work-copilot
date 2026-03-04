package com.workcopilot.ai.dto;

import com.workcopilot.ai.entity.ConversationMessage;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public record MessageDto(
        Long id,
        String role,
        String content,
        List<String> contextSources,
        String model,
        LocalDateTime createdAt
) {
    public static MessageDto from(ConversationMessage entity) {
        List<String> sources = entity.getContextSources() != null && !entity.getContextSources().isBlank()
                ? Arrays.asList(entity.getContextSources().split(","))
                : Collections.emptyList();

        return new MessageDto(
                entity.getId(),
                entity.getRole(),
                entity.getContent(),
                sources,
                entity.getModel(),
                entity.getCreatedAt()
        );
    }
}
