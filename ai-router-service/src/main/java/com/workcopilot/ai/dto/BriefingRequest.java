package com.workcopilot.ai.dto;

import java.util.List;

public record BriefingRequest(
        Long userId,
        List<CalendarEventDto> events,
        List<EmailDto> emails,
        List<DriveFileDto> files
) {
}
