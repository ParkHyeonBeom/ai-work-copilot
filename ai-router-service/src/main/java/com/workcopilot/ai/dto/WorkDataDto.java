package com.workcopilot.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record WorkDataDto(
        List<CalendarEventDto> events,
        List<EmailDto> emails,
        List<DriveFileDto> files,
        LocalDateTime collectedAt
) {
}
