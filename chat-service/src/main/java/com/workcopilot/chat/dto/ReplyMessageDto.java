package com.workcopilot.chat.dto;

public record ReplyMessageDto(
        Long id,
        String senderName,
        String content
) {
}
