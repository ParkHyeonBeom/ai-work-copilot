package com.workcopilot.chat.dto;

public record ParticipantDto(
        Long userId,
        String userName,
        Long lastReadMessageId
) {
}
