package com.workcopilot.integration.service;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.Events;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.common.util.DateTimeUtil;
import com.workcopilot.integration.dto.CalendarEventDto;
import com.workcopilot.integration.google.GoogleCredentialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final GoogleCredentialProvider credentialProvider;
    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;
    @Qualifier("googleApplicationName")
    private final String applicationName;

    public List<CalendarEventDto> getTodayEvents(Long userId) {
        log.info("오늘 일정 조회: userId={}", userId);

        LocalDate today = DateTimeUtil.today();
        LocalDateTime startOfDay = DateTimeUtil.startOfDay(today);
        LocalDateTime endOfDay = DateTimeUtil.endOfDay(today);

        return getEvents(userId, startOfDay, endOfDay);
    }

    public List<CalendarEventDto> getUpcomingEvents(Long userId, int days) {
        log.info("향후 {}일 일정 조회: userId={}", days, userId);

        LocalDate today = DateTimeUtil.today();
        LocalDateTime start = DateTimeUtil.startOfDay(today);
        LocalDateTime end = DateTimeUtil.endOfDay(today.plusDays(days));

        return getEvents(userId, start, end);
    }

    public CalendarEventDto getEventById(Long userId, String eventId) {
        log.info("일정 상세 조회: userId={}, eventId={}", userId, eventId);

        try {
            Calendar calendar = buildCalendarService(userId);
            Event event = calendar.events().get("primary", eventId).execute();
            return convertToDto(event);
        } catch (IOException e) {
            log.error("Google Calendar API 호출 실패: userId={}, eventId={}, error={}",
                    userId, eventId, e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR,
                    "일정 조회에 실패했습니다: " + e.getMessage());
        }
    }

    private List<CalendarEventDto> getEvents(Long userId, LocalDateTime start, LocalDateTime end) {
        try {
            Calendar calendar = buildCalendarService(userId);

            ZoneId zone = DateTimeUtil.getDefaultZone();
            DateTime timeMin = new DateTime(start.atZone(zone).toInstant().toEpochMilli());
            DateTime timeMax = new DateTime(end.atZone(zone).toInstant().toEpochMilli());

            Events events = calendar.events().list("primary")
                    .setTimeMin(timeMin)
                    .setTimeMax(timeMax)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .setMaxResults(50)
                    .execute();

            List<Event> items = events.getItems();
            if (items == null || items.isEmpty()) {
                log.info("조회된 일정 없음: userId={}", userId);
                return Collections.emptyList();
            }

            List<CalendarEventDto> result = items.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            log.info("일정 조회 완료: userId={}, count={}", userId, result.size());
            return result;

        } catch (IOException e) {
            log.error("Google Calendar API 호출 실패: userId={}, error={}", userId, e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR,
                    "일정 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }

    private Calendar buildCalendarService(Long userId) {
        return new Calendar.Builder(httpTransport, jsonFactory, credentialProvider.getCredential(userId))
                .setApplicationName(applicationName)
                .build();
    }

    private CalendarEventDto convertToDto(Event event) {
        boolean isAllDay = event.getStart().getDateTime() == null;

        LocalDateTime startTime;
        LocalDateTime endTime;
        ZoneId zone = DateTimeUtil.getDefaultZone();

        if (isAllDay) {
            startTime = LocalDate.parse(event.getStart().getDate().toStringRfc3339())
                    .atStartOfDay();
            endTime = LocalDate.parse(event.getEnd().getDate().toStringRfc3339())
                    .atStartOfDay();
        } else {
            startTime = Instant.ofEpochMilli(event.getStart().getDateTime().getValue())
                    .atZone(zone)
                    .toLocalDateTime();
            endTime = Instant.ofEpochMilli(event.getEnd().getDateTime().getValue())
                    .atZone(zone)
                    .toLocalDateTime();
        }

        List<String> attendees = Collections.emptyList();
        if (event.getAttendees() != null) {
            attendees = event.getAttendees().stream()
                    .map(EventAttendee::getEmail)
                    .filter(email -> email != null && !email.isBlank())
                    .collect(Collectors.toList());
        }

        return new CalendarEventDto(
                event.getId(),
                event.getSummary(),
                event.getDescription(),
                startTime,
                endTime,
                event.getLocation(),
                attendees,
                isAllDay
        );
    }
}
