package com.workcopilot.integration.service;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.workcopilot.common.audit.AuditAction;
import com.workcopilot.common.audit.Audited;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.common.util.DateTimeUtil;
import com.workcopilot.integration.client.UserInfoClient;
import com.workcopilot.integration.client.UserNotificationClient;
import com.workcopilot.integration.dto.CalendarEventDto;
import com.workcopilot.integration.dto.CreateEventRequest;
import com.workcopilot.integration.entity.CalendarEvent;
import com.workcopilot.integration.entity.CalendarSource;
import com.workcopilot.integration.google.GoogleCredentialProvider;
import com.workcopilot.integration.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private final UserNotificationClient userNotificationClient;
    private final CalendarEventRepository calendarEventRepository;
    private final UserInfoClient userInfoClient;

    @Audited(action = AuditAction.CALENDAR_ACCESSED)
    public List<CalendarEventDto> getTodayEvents(Long userId) {
        log.info("오늘 일정 조회: userId={}", userId);

        LocalDate today = DateTimeUtil.today();
        LocalDateTime startOfDay = DateTimeUtil.startOfDay(today);
        LocalDateTime endOfDay = DateTimeUtil.endOfDay(today);

        return getMergedEvents(userId, startOfDay, endOfDay);
    }

    @Audited(action = AuditAction.CALENDAR_ACCESSED)
    public List<CalendarEventDto> getUpcomingEvents(Long userId, int days) {
        log.info("향후 {}일 일정 조회: userId={}", days, userId);

        LocalDate today = DateTimeUtil.today();
        LocalDateTime start = DateTimeUtil.startOfDay(today);
        LocalDateTime end = DateTimeUtil.endOfDay(today.plusDays(days));

        return getMergedEvents(userId, start, end);
    }

    @Audited(action = AuditAction.CALENDAR_CREATED)
    @Transactional
    public CalendarEventDto createEvent(Long userId, CreateEventRequest request) {
        log.info("일정 생성: userId={}, title={}, source={}", userId, request.title(), request.source());

        CalendarSource source = request.source();

        if (source == CalendarSource.GOOGLE || source == CalendarSource.BOTH) {
            return createGoogleAndLocalEvent(userId, request, source);
        } else {
            return createLocalOnlyEvent(userId, request);
        }
    }

    @Audited(action = AuditAction.CALENDAR_ACCESSED)
    public List<CalendarEventDto> getEventsByRange(Long userId, LocalDateTime start, LocalDateTime end) {
        log.info("범위 일정 조회: userId={}, start={}, end={}", userId, start, end);
        return getMergedEvents(userId, start, end);
    }

    public List<CalendarEventDto> getTeamEvents(Long userId, String role, LocalDateTime start, LocalDateTime end) {
        log.info("팀 일정 조회: userId={}, role={}, start={}, end={}", userId, role, start, end);

        List<CalendarEvent> events;
        if ("ADMIN".equals(role)) {
            events = calendarEventRepository.findByStartTimeBetweenOrderByStartTimeAsc(start, end);
        } else {
            Map<String, Object> userInfo = userInfoClient.getUserInfo(userId);
            String department = userInfo != null ? (String) userInfo.get("department") : null;
            String email = userInfo != null ? (String) userInfo.get("email") : null;

            if (department != null) {
                events = calendarEventRepository
                        .findByCreatorDepartmentAndStartTimeBetweenOrderByStartTimeAsc(department, start, end);
            } else {
                events = Collections.emptyList();
            }

            if (email != null) {
                List<CalendarEvent> allEvents = calendarEventRepository
                        .findByStartTimeBetweenOrderByStartTimeAsc(start, end);
                List<CalendarEvent> deptEvents = events;
                List<CalendarEvent> attendeeEvents = allEvents.stream()
                        .filter(e -> e.getAttendeeEmails() != null && e.getAttendeeEmails().contains(email))
                        .filter(e -> !deptEvents.contains(e))
                        .toList();
                events = new ArrayList<>(events);
                events.addAll(attendeeEvents);
                events.sort(Comparator.comparing(CalendarEvent::getStartTime));
            }
        }

        log.info("팀 일정 조회 완료: userId={}, count={}", userId, events.size());
        return events.stream().map(this::convertLocalToDto).toList();
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

    @Transactional
    public CalendarEventDto updateEvent(Long userId, Long eventId, CreateEventRequest request) {
        log.info("일정 수정: userId={}, eventId={}", userId, eventId);

        CalendarEvent event = calendarEventRepository.findByIdAndCreatorUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "일정을 찾을 수 없습니다."));

        event.update(request.title(), request.description(), request.startTime(), request.endTime(),
                request.location(), request.isAllDay(), request.attendeeEmails());

        return convertLocalToDto(event);
    }

    @Transactional
    public void deleteEvent(Long userId, Long eventId) {
        log.info("일정 삭제: userId={}, eventId={}", userId, eventId);

        CalendarEvent event = calendarEventRepository.findByIdAndCreatorUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "일정을 찾을 수 없습니다."));

        calendarEventRepository.delete(event);
    }

    /**
     * Google Calendar + 로컬 DB 이벤트를 병합하여 반환 (googleEventId 기준 중복 제거)
     */
    private List<CalendarEventDto> getMergedEvents(Long userId, LocalDateTime start, LocalDateTime end) {
        List<CalendarEventDto> googleEvents = getGoogleEvents(userId, start, end);
        List<CalendarEvent> localEvents = calendarEventRepository
                .findByCreatorUserIdAndStartTimeBetweenOrderByStartTimeAsc(userId, start, end);

        Set<String> googleEventIds = googleEvents.stream()
                .map(CalendarEventDto::id)
                .collect(Collectors.toSet());

        List<CalendarEventDto> localOnlyEvents = localEvents.stream()
                .filter(e -> e.getGoogleEventId() == null || !googleEventIds.contains(e.getGoogleEventId()))
                .map(this::convertLocalToDto)
                .toList();

        List<CalendarEventDto> merged = new ArrayList<>(googleEvents);
        merged.addAll(localOnlyEvents);
        merged.sort(Comparator.comparing(CalendarEventDto::startTime));

        log.info("병합 일정 조회 완료: userId={}, google={}, localOnly={}, total={}",
                userId, googleEvents.size(), localOnlyEvents.size(), merged.size());
        return merged;
    }

    private List<CalendarEventDto> getGoogleEvents(Long userId, LocalDateTime start, LocalDateTime end) {
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
                return Collections.emptyList();
            }

            return items.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.warn("Google Calendar API 호출 실패, 로컬 이벤트만 반환: userId={}, error={}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private CalendarEventDto createGoogleAndLocalEvent(Long userId, CreateEventRequest request, CalendarSource source) {
        try {
            Calendar calendar = buildCalendarService(userId);
            ZoneId zone = DateTimeUtil.getDefaultZone();

            Event event = new Event()
                    .setSummary(request.title())
                    .setDescription(request.description())
                    .setLocation(request.location());

            if (request.isAllDay()) {
                event.setStart(new EventDateTime()
                        .setDate(new DateTime(request.startTime().toLocalDate().toString())));
                event.setEnd(new EventDateTime()
                        .setDate(new DateTime(request.endTime().toLocalDate().toString())));
            } else {
                event.setStart(new EventDateTime()
                        .setDateTime(new DateTime(request.startTime().atZone(zone).toInstant().toEpochMilli()))
                        .setTimeZone(zone.getId()));
                event.setEnd(new EventDateTime()
                        .setDateTime(new DateTime(request.endTime().atZone(zone).toInstant().toEpochMilli()))
                        .setTimeZone(zone.getId()));
            }

            if (request.attendeeEmails() != null && !request.attendeeEmails().isEmpty()) {
                List<EventAttendee> attendees = request.attendeeEmails().stream()
                        .map(email -> new EventAttendee().setEmail(email))
                        .toList();
                event.setAttendees(attendees);
            }

            Event created = calendar.events().insert("primary", event).execute();
            log.info("Google Calendar 일정 생성 완료: userId={}, eventId={}", userId, created.getId());

            saveToLocalDb(userId, request, created.getId(), source);

            String meetingTime = request.startTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            userNotificationClient.sendMeetingNotification(
                    userId, request.title(), meetingTime, request.location(), request.attendeeEmails());

            return convertToDto(created);

        } catch (IOException e) {
            log.error("Google Calendar 일정 생성 실패: userId={}, error={}", userId, e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR,
                    "일정 생성에 실패했습니다: " + e.getMessage());
        }
    }

    private CalendarEventDto createLocalOnlyEvent(Long userId, CreateEventRequest request) {
        CalendarEvent saved = saveToLocalDb(userId, request, null, CalendarSource.LOCAL);
        log.info("사내 캘린더 일정 생성 완료: userId={}, eventId={}", userId, saved.getId());

        String meetingTime = request.startTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        userNotificationClient.sendMeetingNotification(
                userId, request.title(), meetingTime, request.location(), request.attendeeEmails());

        return convertLocalToDto(saved);
    }

    private CalendarEvent saveToLocalDb(Long userId, CreateEventRequest request, String googleEventId, CalendarSource source) {
        Map<String, Object> userInfo = userInfoClient.getUserInfo(userId);
        String email = userInfo != null ? (String) userInfo.get("email") : null;
        String department = userInfo != null ? (String) userInfo.get("department") : null;

        CalendarEvent localEvent = CalendarEvent.builder()
                .creatorUserId(userId)
                .creatorEmail(email)
                .creatorDepartment(department)
                .title(request.title())
                .description(request.description())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .location(request.location())
                .isAllDay(request.isAllDay())
                .attendeeEmails(request.attendeeEmails() != null ? request.attendeeEmails() : List.of())
                .googleEventId(googleEventId)
                .source(source)
                .build();

        return calendarEventRepository.save(localEvent);
    }

    private CalendarEventDto convertLocalToDto(CalendarEvent event) {
        return new CalendarEventDto(
                event.getId().toString(),
                event.getTitle(),
                event.getDescription(),
                event.getStartTime(),
                event.getEndTime(),
                event.getLocation(),
                event.getAttendeeEmails() != null ? event.getAttendeeEmails() : Collections.emptyList(),
                event.isAllDay(),
                event.getSource() != null ? event.getSource().name() : "LOCAL"
        );
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
                isAllDay,
                "GOOGLE"
        );
    }
}
