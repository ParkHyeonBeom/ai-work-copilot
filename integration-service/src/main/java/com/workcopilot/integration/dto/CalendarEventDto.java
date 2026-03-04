package com.workcopilot.integration.dto;

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
        boolean isAllDay,
        String source
) {
    public CalendarEventDto(String id, String title, String description, LocalDateTime startTime,
                            LocalDateTime endTime, String location, List<String> attendees, boolean isAllDay) {
        this(id, title, description, startTime, endTime, location, attendees, isAllDay, "GOOGLE");
    }
}
