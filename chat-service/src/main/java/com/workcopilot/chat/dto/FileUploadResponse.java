package com.workcopilot.chat.dto;

public record FileUploadResponse(
        Long fileId,
        String originalFileName,
        String mimeType,
        Long fileSize
) {
}
