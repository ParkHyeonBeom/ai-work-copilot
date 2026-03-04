package com.workcopilot.chat.dto;

public record ChatFileDto(
        Long id,
        String originalFileName,
        String mimeType,
        Long fileSize
) {
}
