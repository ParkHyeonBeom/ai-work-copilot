package com.workcopilot.integration.repository;

import com.workcopilot.integration.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByStartTimeBetweenOrderByStartTimeAsc(LocalDateTime start, LocalDateTime end);

    List<CalendarEvent> findByCreatorDepartmentAndStartTimeBetweenOrderByStartTimeAsc(
            String department, LocalDateTime start, LocalDateTime end);
}
