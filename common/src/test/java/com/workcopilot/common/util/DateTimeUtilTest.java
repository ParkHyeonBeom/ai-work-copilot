package com.workcopilot.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilTest {

    @Test
    void now_서울시간대_현재시각반환() {
        LocalDateTime now = DateTimeUtil.now();

        assertThat(now).isNotNull();
        assertThat(now).isBeforeOrEqualTo(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    }

    @Test
    void today_서울시간대_오늘날짜반환() {
        LocalDate today = DateTimeUtil.today();

        assertThat(today).isEqualTo(LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    @Test
    void startOfDay_특정날짜_자정반환() {
        LocalDate date = LocalDate.of(2026, 2, 24);

        LocalDateTime start = DateTimeUtil.startOfDay(date);

        assertThat(start).isEqualTo(LocalDateTime.of(2026, 2, 24, 0, 0, 0));
    }

    @Test
    void endOfDay_특정날짜_235959반환() {
        LocalDate date = LocalDate.of(2026, 2, 24);

        LocalDateTime end = DateTimeUtil.endOfDay(date);

        assertThat(end.toLocalDate()).isEqualTo(date);
        assertThat(end.toLocalTime()).isEqualTo(LocalTime.MAX);
    }

    @Test
    void formatDate_날짜포맷_yyyyMMdd() {
        LocalDate date = LocalDate.of(2026, 2, 24);

        String formatted = DateTimeUtil.formatDate(date);

        assertThat(formatted).isEqualTo("2026-02-24");
    }

    @Test
    void formatDateTime_날짜시간포맷_yyyyMMddHHmmss() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 2, 24, 9, 30, 0);

        String formatted = DateTimeUtil.formatDateTime(dateTime);

        assertThat(formatted).isEqualTo("2026-02-24 09:30:00");
    }

    @Test
    void isWorkingHour_근무시간내_true() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 2, 24, 10, 0, 0);

        boolean result = DateTimeUtil.isWorkingHour(dateTime, "09:00", "18:00");

        assertThat(result).isTrue();
    }

    @Test
    void isWorkingHour_근무시간외_false() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 2, 24, 7, 0, 0);

        boolean result = DateTimeUtil.isWorkingHour(dateTime, "09:00", "18:00");

        assertThat(result).isFalse();
    }

    @Test
    void isWorkingHour_경계시간_true() {
        LocalDateTime startBoundary = LocalDateTime.of(2026, 2, 24, 9, 0, 0);
        LocalDateTime endBoundary = LocalDateTime.of(2026, 2, 24, 18, 0, 0);

        assertThat(DateTimeUtil.isWorkingHour(startBoundary, "09:00", "18:00")).isTrue();
        assertThat(DateTimeUtil.isWorkingHour(endBoundary, "09:00", "18:00")).isTrue();
    }

    @Test
    void getDefaultZone_서울타임존() {
        assertThat(DateTimeUtil.getDefaultZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
    }
}
