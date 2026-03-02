package com.workcopilot.integration.entity;

import com.workcopilot.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "calendar_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CalendarEvent extends BaseEntity {

    @Column(nullable = false)
    private Long creatorUserId;

    private String creatorEmail;

    private String creatorDepartment;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    private String location;

    @Builder.Default
    private boolean isAllDay = false;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> attendeeEmails = List.of();

    private String googleEventId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private CalendarSource source = CalendarSource.GOOGLE;

    public void update(String title, String description, LocalDateTime startTime, LocalDateTime endTime,
                       String location, boolean isAllDay, List<String> attendeeEmails) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.isAllDay = isAllDay;
        this.attendeeEmails = attendeeEmails != null ? attendeeEmails : List.of();
    }
}
