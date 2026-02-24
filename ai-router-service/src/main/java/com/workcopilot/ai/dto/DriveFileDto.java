package com.workcopilot.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DriveFileDto(
        String id,
        String name,
        String mimeType,
        LocalDateTime modifiedTime,
        List<String> owners,
        String webViewLink
) {
}
