package com.workcopilot.integration.dto;

import com.workcopilot.integration.entity.CalendarSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateEventRequest(
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        String location,
        List<String> attendeeEmails,
        boolean isAllDay,
        CalendarSource source
) {
    public CalendarSource source() {
        return source != null ? source : CalendarSource.GOOGLE;
    }
}
