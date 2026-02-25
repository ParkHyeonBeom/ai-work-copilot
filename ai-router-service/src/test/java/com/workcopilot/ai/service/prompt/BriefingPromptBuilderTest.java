package com.workcopilot.ai.service.prompt;

import com.workcopilot.ai.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BriefingPromptBuilderTest {

    @InjectMocks
    private BriefingPromptBuilder briefingPromptBuilder;

    @Test
    @DisplayName("build_일정이메일파일모두있는요청_모든섹션포함된프롬프트생성")
    void build_일정이메일파일모두있는요청_모든섹션포함된프롬프트생성() {
        // given
        BriefingRequest request = createFullBriefingRequest();

        // when
        String prompt = briefingPromptBuilder.build(request);

        // then
        assertThat(prompt).isNotNull();
        assertThat(prompt).contains("오늘의 일정 (Google Calendar)");
        assertThat(prompt).contains("주요 이메일 (Gmail)");
        assertThat(prompt).contains("최근 수정된 파일 (Google Drive)");
        assertThat(prompt).contains("팀 스탠드업 회의");
        assertThat(prompt).contains("프로젝트 진행상황 보고 요청");
        assertThat(prompt).contains("프로젝트 기획서.docx");
    }

    @Test
    @DisplayName("build_빈요청_빈데이터안내메시지포함된프롬프트생성")
    void build_빈요청_빈데이터안내메시지포함된프롬프트생성() {
        // given
        BriefingRequest request = new BriefingRequest(1L, List.of(), List.of(), List.of());

        // when
        String prompt = briefingPromptBuilder.build(request);

        // then
        assertThat(prompt).isNotNull();
        assertThat(prompt).contains("오늘 등록된 일정이 없습니다");
        assertThat(prompt).contains("새로운 이메일이 없습니다");
        assertThat(prompt).contains("최근 수정된 파일이 없습니다");
    }

    @Test
    @DisplayName("build_null데이터요청_빈데이터안내메시지포함")
    void build_null데이터요청_빈데이터안내메시지포함() {
        // given
        BriefingRequest request = new BriefingRequest(1L, null, null, null);

        // when
        String prompt = briefingPromptBuilder.build(request);

        // then
        assertThat(prompt).isNotNull();
        assertThat(prompt).contains("오늘 등록된 일정이 없습니다");
        assertThat(prompt).contains("새로운 이메일이 없습니다");
        assertThat(prompt).contains("최근 수정된 파일이 없습니다");
    }

    @Test
    @DisplayName("build_종일일정포함_시간표시가종일로나옴")
    void build_종일일정포함_시간표시가종일로나옴() {
        // given
        CalendarEventDto allDayEvent = new CalendarEventDto(
                "event-1", "연차", null,
                LocalDateTime.of(2026, 2, 24, 0, 0),
                LocalDateTime.of(2026, 2, 25, 0, 0),
                null, List.of(), true
        );
        BriefingRequest request = new BriefingRequest(
                1L, List.of(allDayEvent), List.of(), List.of()
        );

        // when
        String prompt = briefingPromptBuilder.build(request);

        // then
        assertThat(prompt).contains("시간: 종일");
    }

    @Test
    @DisplayName("build_중요이메일포함_중요표시포함")
    void build_중요이메일포함_중요표시포함() {
        // given
        EmailDto importantEmail = new EmailDto(
                "email-1", "boss@example.com", "긴급 보고 요청",
                "지금 바로 보고서를 제출해주세요.",
                LocalDateTime.of(2026, 2, 24, 9, 0),
                List.of("INBOX", "IMPORTANT"),
                true
        );
        BriefingRequest request = new BriefingRequest(
                1L, List.of(), List.of(importantEmail), List.of()
        );

        // when
        String prompt = briefingPromptBuilder.build(request);

        // then
        assertThat(prompt).contains("[중요]");
        assertThat(prompt).contains("긴급 보고 요청");
    }

    @Test
    @DisplayName("getSystemPrompt_항상동일한시스템프롬프트반환")
    void getSystemPrompt_항상동일한시스템프롬프트반환() {
        // when
        String systemPrompt = briefingPromptBuilder.getSystemPrompt();

        // then
        assertThat(systemPrompt).isNotNull();
        assertThat(systemPrompt).contains("사내 업무 브리핑 AI");
        assertThat(systemPrompt).contains("JSON");
    }

    private BriefingRequest createFullBriefingRequest() {
        List<CalendarEventDto> events = List.of(
                new CalendarEventDto(
                        "event-1", "팀 스탠드업 회의", "주간 업무 공유",
                        LocalDateTime.of(2026, 2, 24, 10, 0),
                        LocalDateTime.of(2026, 2, 24, 10, 30),
                        "회의실 A",
                        List.of("alice@example.com", "bob@example.com"),
                        false
                ),
                new CalendarEventDto(
                        "event-2", "1:1 미팅", "분기 목표 논의",
                        LocalDateTime.of(2026, 2, 24, 14, 0),
                        LocalDateTime.of(2026, 2, 24, 15, 0),
                        "온라인",
                        List.of("manager@example.com"),
                        false
                )
        );

        List<EmailDto> emails = List.of(
                new EmailDto(
                        "email-1", "manager@example.com", "프로젝트 진행상황 보고 요청",
                        "이번 주 금요일까지 프로젝트 진행상황을 보고해주세요.",
                        LocalDateTime.of(2026, 2, 24, 8, 30),
                        List.of("INBOX", "IMPORTANT"),
                        true
                ),
                new EmailDto(
                        "email-2", "hr@example.com", "2월 급여 명세서",
                        "2월 급여 명세서가 발행되었습니다.",
                        LocalDateTime.of(2026, 2, 24, 9, 0),
                        List.of("INBOX"),
                        false
                )
        );

        List<DriveFileDto> files = List.of(
                new DriveFileDto(
                        "file-1", "프로젝트 기획서.docx",
                        "application/vnd.google-apps.document",
                        LocalDateTime.of(2026, 2, 23, 17, 0),
                        List.of("alice@example.com"),
                        "https://docs.google.com/document/d/1"
                )
        );

        return new BriefingRequest(1L, events, emails, files);
    }
}
