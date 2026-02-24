package com.workcopilot.briefing.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CalendarEventDto(
        String id,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        List<String> attendees,
        boolean isAllDay
) {
}
