package com.workcopilot.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workcopilot.ai.dto.*;
import com.workcopilot.ai.service.prompt.BriefingPromptBuilder;
import com.workcopilot.ai.service.prompt.ClassificationPromptBuilder;
import com.workcopilot.ai.service.prompt.SummarizationPromptBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private LlmRouter llmRouter;

    @Mock
    private BriefingPromptBuilder briefingPromptBuilder;

    @Mock
    private ClassificationPromptBuilder classificationPromptBuilder;

    @Mock
    private SummarizationPromptBuilder summarizationPromptBuilder;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AiService aiService;

    @Test
    @DisplayName("generateBriefing_유효한요청_브리핑응답반환")
    void generateBriefing_유효한요청_브리핑응답반환() {
        // given
        BriefingRequest request = createTestBriefingRequest();
        String mockPrompt = "테스트 프롬프트";
        String llmResult = """
                {
                  "summary": "오늘 3개의 회의와 5개의 이메일이 있습니다.",
                  "fullContent": "상세 브리핑 내용",
                  "keyPoints": ["회의 준비 필요", "이메일 확인 필요"],
                  "actionItems": ["10시 회의 자료 준비", "프로젝트 보고서 검토"]
                }
                """;

        given(briefingPromptBuilder.build(request)).willReturn(mockPrompt);
        given(llmRouter.route(eq("briefing"), eq(mockPrompt)))
                .willReturn(new AiResponse(llmResult, "gpt-4o", 1500));

        // when
        BriefingResponse response = aiService.generateBriefing(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.summary()).isEqualTo("오늘 3개의 회의와 5개의 이메일이 있습니다.");
        assertThat(response.keyPoints()).hasSize(2);
        assertThat(response.actionItems()).hasSize(2);
        assertThat(response.actionItems()).contains("10시 회의 자료 준비");

        verify(briefingPromptBuilder).build(request);
        verify(llmRouter).route("briefing", mockPrompt);
    }

    @Test
    @DisplayName("generateBriefing_JSON파싱실패시_원문을fullContent로반환")
    void generateBriefing_JSON파싱실패시_원문을fullContent로반환() {
        // given
        BriefingRequest request = createTestBriefingRequest();
        String mockPrompt = "테스트 프롬프트";
        String invalidJsonResult = "이것은 JSON이 아닌 일반 텍스트 응답입니다.";

        given(briefingPromptBuilder.build(request)).willReturn(mockPrompt);
        given(llmRouter.route(eq("briefing"), eq(mockPrompt)))
                .willReturn(new AiResponse(invalidJsonResult, "gpt-4o", 1200));

        // when
        BriefingResponse response = aiService.generateBriefing(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.summary()).isEqualTo("브리핑이 생성되었습니다.");
        assertThat(response.fullContent()).isEqualTo(invalidJsonResult);
        assertThat(response.keyPoints()).isEmpty();
        assertThat(response.actionItems()).isEmpty();
    }

    @Test
    @DisplayName("classifyContent_유효한콘텐츠_분류결과반환")
    void classifyContent_유효한콘텐츠_분류결과반환() {
        // given
        String content = "긴급: 내일 오전 10시까지 프로젝트 보고서를 제출해주세요.";
        String mockPrompt = "분류 프롬프트";
        AiResponse expectedResponse = new AiResponse(
                "{\"importance\": \"high\", \"category\": \"urgent\"}",
                "ollama-llama3.1-8b",
                300
        );

        given(classificationPromptBuilder.build(content)).willReturn(mockPrompt);
        given(llmRouter.route(eq("classify"), eq(mockPrompt))).willReturn(expectedResponse);

        // when
        AiResponse response = aiService.classifyContent(content);

        // then
        assertThat(response).isNotNull();
        assertThat(response.result()).contains("high");
        assertThat(response.result()).contains("urgent");

        verify(classificationPromptBuilder).build(content);
        verify(llmRouter).route("classify", mockPrompt);
    }

    @Test
    @DisplayName("summarizeDocument_유효한문서_요약결과반환")
    void summarizeDocument_유효한문서_요약결과반환() {
        // given
        String content = "이것은 매우 긴 문서의 내용입니다. 여러 페이지에 걸쳐 작성되었습니다.";
        String mockPrompt = "요약 프롬프트";
        AiResponse expectedResponse = new AiResponse(
                "{\"summary\": \"긴 문서의 핵심 요약\"}",
                "gpt-4o",
                2000
        );

        given(summarizationPromptBuilder.build(content)).willReturn(mockPrompt);
        given(llmRouter.route(eq("summarize"), eq(mockPrompt))).willReturn(expectedResponse);

        // when
        AiResponse response = aiService.summarizeDocument(content);

        // then
        assertThat(response).isNotNull();
        assertThat(response.model()).isEqualTo("gpt-4o");
        assertThat(response.result()).contains("핵심 요약");

        verify(summarizationPromptBuilder).build(content);
        verify(llmRouter).route("summarize", mockPrompt);
    }

    private BriefingRequest createTestBriefingRequest() {
        List<CalendarEventDto> events = List.of(
                new CalendarEventDto(
                        "event-1", "팀 스탠드업 회의", "주간 업무 공유",
                        LocalDateTime.of(2026, 2, 24, 10, 0),
                        LocalDateTime.of(2026, 2, 24, 10, 30),
                        "회의실 A",
                        List.of("alice@example.com", "bob@example.com"),
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
