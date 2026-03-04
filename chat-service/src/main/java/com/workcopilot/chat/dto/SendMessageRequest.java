package com.workcopilot.chat.dto;

import com.workcopilot.chat.entity.ChatMessageType;

public record SendMessageRequest(
        ChatMessageType type,
        String content,
        Long replyToMessageId
) {
    public SendMessageRequest {
        if (type == null) {
            type = ChatMessageType.TEXT;
        }
    }
}
