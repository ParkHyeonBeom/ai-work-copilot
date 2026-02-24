package com.workcopilot.integration.service;

import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.common.util.DateTimeUtil;
import com.workcopilot.integration.dto.CalendarEventDto;
import com.workcopilot.integration.dto.DriveFileDto;
import com.workcopilot.integration.dto.EmailDto;
import com.workcopilot.integration.dto.WorkDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataCollectorService {

    private static final int DEFAULT_EMAIL_MAX = 20;
    private static final int DEFAULT_FILE_MAX = 20;
    private static final long TIMEOUT_SECONDS = 30;

    private final CalendarService calendarService;
    private final GmailService gmailService;
    private final DriveService driveService;

    public WorkDataDto collectAll(Long userId) {
        log.info("전체 업무 데이터 수집 시작: userId={}", userId);

        CompletableFuture<List<CalendarEventDto>> eventsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return calendarService.getTodayEvents(userId);
            } catch (Exception e) {
                log.error("일정 수집 실패: userId={}, error={}", userId, e.getMessage());
                return Collections.emptyList();
            }
        });

        CompletableFuture<List<EmailDto>> emailsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return gmailService.getRecentEmails(userId, DEFAULT_EMAIL_MAX);
            } catch (Exception e) {
                log.error("이메일 수집 실패: userId={}, error={}", userId, e.getMessage());
                return Collections.emptyList();
            }
        });

        CompletableFuture<List<DriveFileDto>> filesFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return driveService.getRecentFiles(userId, DEFAULT_FILE_MAX);
            } catch (Exception e) {
                log.error("파일 수집 실패: userId={}, error={}", userId, e.getMessage());
                return Collections.emptyList();
            }
        });

        try {
            CompletableFuture.allOf(eventsFuture, emailsFuture, filesFuture)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            List<CalendarEventDto> events = eventsFuture.get();
            List<EmailDto> emails = emailsFuture.get();
            List<DriveFileDto> files = filesFuture.get();

            WorkDataDto result = new WorkDataDto(events, emails, files, DateTimeUtil.now());

            log.info("전체 업무 데이터 수집 완료: userId={}, events={}, emails={}, files={}",
                    userId, events.size(), emails.size(), files.size());

            return result;

        } catch (TimeoutException e) {
            log.error("업무 데이터 수집 타임아웃: userId={}", userId);
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR,
                    "데이터 수집 시간이 초과되었습니다.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("업무 데이터 수집 중단: userId={}", userId);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "데이터 수집이 중단되었습니다.");
        } catch (ExecutionException e) {
            log.error("업무 데이터 수집 실패: userId={}, error={}", userId, e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR,
                    "데이터 수집에 실패했습니다: " + e.getMessage());
        }
    }
}
