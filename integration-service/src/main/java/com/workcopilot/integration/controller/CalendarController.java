package com.workcopilot.integration.controller;

import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.integration.dto.CalendarEventDto;
import com.workcopilot.integration.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integrations/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/events/today")
    public ApiResponse<List<CalendarEventDto>> getTodayEvents(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(calendarService.getTodayEvents(userId));
    }

    @GetMapping("/events")
    public ApiResponse<List<CalendarEventDto>> getUpcomingEvents(
            Authentication authentication,
            @RequestParam(defaultValue = "7") int days) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(calendarService.getUpcomingEvents(userId, days));
    }

    @GetMapping("/events/{eventId}")
    public ApiResponse<CalendarEventDto> getEventById(
            Authentication authentication,
            @PathVariable String eventId) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(calendarService.getEventById(userId, eventId));
    }
}
