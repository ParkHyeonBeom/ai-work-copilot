package com.workcopilot.chat.dto;

import com.workcopilot.chat.entity.ChatMessageType;
import java.time.LocalDateTime;

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
        LocalDateTime createdAt
) {
}
