package com.workcopilot.integration.controller;

import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.integration.dto.CalendarEventDto;
import com.workcopilot.integration.dto.CreateEventRequest;
import com.workcopilot.integration.service.CalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @GetMapping("/events/range")
    public ApiResponse<List<CalendarEventDto>> getEventsByRange(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(calendarService.getEventsByRange(userId, start, end));
    }

    @GetMapping("/events/team")
    public ApiResponse<List<CalendarEventDto>> getTeamEvents(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("USER");
        return ApiResponse.ok(calendarService.getTeamEvents(userId, role, start, end));
    }

    @GetMapping("/events/{eventId}")
    public ApiResponse<CalendarEventDto> getEventById(
            Authentication authentication,
            @PathVariable String eventId) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(calendarService.getEventById(userId, eventId));
    }

    @PostMapping("/events")
    public ApiResponse<CalendarEventDto> createEvent(
            Authentication authentication,
            @Valid @RequestBody CreateEventRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(calendarService.createEvent(userId, request));
    }
}
