package com.workcopilot.chat.dto;

import com.workcopilot.chat.entity.ChatMessageType;
import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageDto(
        Long id,
        Long chatRoomId,
        Long senderUserId,
        String senderName,
        ChatMessageType type,
        String content,
        ChatFileDto file,
        boolean deleted,
        ReplyMessageDto replyTo,
        List<ReactionDto> reactions,
        LocalDateTime editedAt,
        LocalDateTime createdAt
) {
}
