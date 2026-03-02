package com.workcopilot.ai.dto;

import com.workcopilot.ai.entity.ConversationHistory;

import java.time.LocalDateTime;

public record ConversationDto(
        Long id,
        String title,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ConversationDto from(ConversationHistory entity) {
        return new ConversationDto(
                entity.getId(),
                entity.getTitle(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
