package com.workcopilot.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EmailDto(
        String id,
        String from,
        String subject,
        String snippet,
        LocalDateTime receivedAt,
        List<String> labels,
        boolean isImportant,
        boolean isRead
) {
}
