package com.workcopilot.integration.service;

import com.workcopilot.integration.dto.CalendarEventDto;
import com.workcopilot.integration.dto.DriveFileDto;
import com.workcopilot.integration.dto.EmailDto;
import com.workcopilot.integration.dto.WorkDataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataCollectorServiceTest {

    @Mock
    private CalendarService calendarService;

    @Mock
    private GmailService gmailService;

    @Mock
    private DriveService driveService;

    @InjectMocks
    private DataCollectorService dataCollectorService;

    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("collectAll_정상호출_모든데이터수집")
    void collectAll_정상호출_모든데이터수집() {
        // given
        List<CalendarEventDto> events = List.of(
                new CalendarEventDto("e1", "회의", "설명",
                        LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                        "회의실A", List.of("user@test.com"), false)
        );

        List<EmailDto> emails = List.of(
                new EmailDto("m1", "sender@test.com", "제목", "내용 미리보기",
                        LocalDateTime.now(), List.of("INBOX"), false, true)
        );

        List<DriveFileDto> files = List.of(
                new DriveFileDto("f1", "문서.docx", "application/vnd.google-apps.document",
                        LocalDateTime.now(), List.of("owner@test.com"),
                        "https://drive.google.com/file/f1")
        );

        given(calendarService.getTodayEvents(USER_ID)).willReturn(events);
        given(gmailService.getRecentEmails(anyLong(), anyInt())).willReturn(emails);
        given(driveService.getRecentFiles(anyLong(), anyInt())).willReturn(files);

        // when
        WorkDataDto result = dataCollectorService.collectAll(USER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.events()).hasSize(1);
        assertThat(result.emails()).hasSize(1);
        assertThat(result.files()).hasSize(1);
        assertThat(result.collectedAt()).isNotNull();

        assertThat(result.events().get(0).title()).isEqualTo("회의");
        assertThat(result.emails().get(0).subject()).isEqualTo("제목");
        assertThat(result.files().get(0).name()).isEqualTo("문서.docx");
    }

    @Test
    @DisplayName("collectAll_일부서비스실패_빈목록으로대체")
    void collectAll_일부서비스실패_빈목록으로대체() {
        // given
        given(calendarService.getTodayEvents(USER_ID))
                .willThrow(new RuntimeException("Calendar API 오류"));

        List<EmailDto> emails = List.of(
                new EmailDto("m1", "sender@test.com", "제목", "내용",
                        LocalDateTime.now(), List.of("INBOX"), false, true)
        );
        given(gmailService.getRecentEmails(anyLong(), anyInt())).willReturn(emails);

        List<DriveFileDto> files = List.of(
                new DriveFileDto("f1", "파일.txt", "text/plain",
                        LocalDateTime.now(), List.of("owner"), "https://link")
        );
        given(driveService.getRecentFiles(anyLong(), anyInt())).willReturn(files);

        // when
        WorkDataDto result = dataCollectorService.collectAll(USER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.events()).isEmpty();
        assertThat(result.emails()).hasSize(1);
        assertThat(result.files()).hasSize(1);
    }

    @Test
    @DisplayName("collectAll_모든서비스빈결과_빈WorkDataDto반환")
    void collectAll_모든서비스빈결과_빈WorkDataDto반환() {
        // given
        given(calendarService.getTodayEvents(USER_ID)).willReturn(Collections.emptyList());
        given(gmailService.getRecentEmails(anyLong(), anyInt())).willReturn(Collections.emptyList());
        given(driveService.getRecentFiles(anyLong(), anyInt())).willReturn(Collections.emptyList());

        // when
        WorkDataDto result = dataCollectorService.collectAll(USER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.events()).isEmpty();
        assertThat(result.emails()).isEmpty();
        assertThat(result.files()).isEmpty();
        assertThat(result.collectedAt()).isNotNull();
    }

    @Test
    @DisplayName("collectAll_정상호출_각서비스호출확인")
    void collectAll_정상호출_각서비스호출확인() {
        // given
        given(calendarService.getTodayEvents(USER_ID)).willReturn(Collections.emptyList());
        given(gmailService.getRecentEmails(anyLong(), anyInt())).willReturn(Collections.emptyList());
        given(driveService.getRecentFiles(anyLong(), anyInt())).willReturn(Collections.emptyList());

        // when
        dataCollectorService.collectAll(USER_ID);

        // then
        verify(calendarService).getTodayEvents(USER_ID);
        verify(gmailService).getRecentEmails(USER_ID, 20);
        verify(driveService).getRecentFiles(USER_ID, 20);
    }
}
