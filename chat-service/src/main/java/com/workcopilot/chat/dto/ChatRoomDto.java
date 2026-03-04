package com.workcopilot.chat.dto;

import com.workcopilot.chat.entity.ChatRoomType;

import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomDto(
        Long id,
        String name,
        ChatRoomType type,
        Long creatorUserId,
        String lastMessageContent,
        LocalDateTime lastMessageAt,
        List<ParticipantDto> participants,
        int unreadCount
) {
}
