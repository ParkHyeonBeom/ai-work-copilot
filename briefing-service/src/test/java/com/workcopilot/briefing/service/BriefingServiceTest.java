package com.workcopilot.briefing.service;

import com.workcopilot.briefing.client.AiRouterClient;
import com.workcopilot.briefing.client.IntegrationClient;
import com.workcopilot.briefing.dto.*;
import com.workcopilot.briefing.entity.Briefing;
import com.workcopilot.briefing.entity.BriefingStatus;
import com.workcopilot.briefing.repository.BriefingRepository;
import com.workcopilot.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BriefingServiceTest {

    @Mock
    private BriefingRepository briefingRepository;

    @Mock
    private IntegrationClient integrationClient;

    @Mock
    private AiRouterClient aiRouterClient;

    @InjectMocks
    private BriefingService briefingService;

    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("generateDailyBriefing_신규생성_정상응답")
    void generateDailyBriefing_신규생성_정상응답() {
        // given
        given(briefingRepository.findByUserIdAndBriefingDate(USER_ID, LocalDate.now()))
                .willReturn(Optional.empty());

        Briefing savedBriefing = Briefing.builder()
                .userId(USER_ID)
                .briefingDate(LocalDate.now())
                .status(BriefingStatus.PENDING)
                .build();
        given(briefingRepository.save(any(Briefing.class))).willReturn(savedBriefing);

        WorkDataDto workData = new WorkDataDto(
                List.of(new CalendarEventDto("e1", "회의", "설명",
                        LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                        "회의실A", List.of("user@test.com"), false)),
                List.of(new EmailDto("m1", "sender@test.com", "제목", "내용",
                        LocalDateTime.now(), List.of("INBOX"), false, true)),
                List.of(new DriveFileDto("f1", "문서.docx", "application/vnd.google-apps.document",
                        LocalDateTime.now(), List.of("owner@test.com"), "https://link")),
                LocalDateTime.now()
        );
        given(integrationClient.collectWorkData()).willReturn(workData);

        BriefingAiResponse aiResponse = new BriefingAiResponse(
                "오늘의 브리핑 요약",
                "오늘의 상세 브리핑 내용입니다. 오전 회의와 이메일을 확인하세요.",
                List.of("오전 회의 참석", "이메일 확인 필요"),
                List.of("회의 자료 준비", "이메일 회신")
        );
        given(aiRouterClient.generateBriefing(any(BriefingRequest.class))).willReturn(aiResponse);

        // when
        BriefingResponse response = briefingService.generateDailyBriefing(USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.status()).isEqualTo(BriefingStatus.COMPLETED);
        assertThat(response.summary()).isEqualTo("오늘의 브리핑 요약");
        assertThat(response.keyPoints()).hasSize(2);
        assertThat(response.actionItems()).hasSize(2);
        assertThat(response.eventCount()).isEqualTo(1);
        assertThat(response.emailCount()).isEqualTo(1);
        assertThat(response.fileCount()).isEqualTo(1);

        verify(integrationClient).collectWorkData();
        verify(aiRouterClient).generateBriefing(any(BriefingRequest.class));
    }

    @Test
    @DisplayName("generateDailyBriefing_이미존재_기존반환")
    void generateDailyBriefing_이미존재_기존반환() {
        // given
        Briefing existingBriefing = Briefing.builder()
                .userId(USER_ID)
                .briefingDate(LocalDate.now())
                .status(BriefingStatus.COMPLETED)
                .build();
        existingBriefing.complete(
                "기존 브리핑 요약",
                "기존 브리핑 내용",
                List.of("포인트1"),
                List.of("액션1"),
                3, 5, 2
        );

        given(briefingRepository.findByUserIdAndBriefingDate(USER_ID, LocalDate.now()))
                .willReturn(Optional.of(existingBriefing));

        // when
        BriefingResponse response = briefingService.generateDailyBriefing(USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(BriefingStatus.COMPLETED);
        assertThat(response.summary()).isEqualTo("기존 브리핑 요약");
        assertThat(response.eventCount()).isEqualTo(3);
        assertThat(response.emailCount()).isEqualTo(5);
        assertThat(response.fileCount()).isEqualTo(2);

        // 외부 서비스 호출하지 않음
        verify(integrationClient, never()).collectWorkData();
        verify(aiRouterClient, never()).generateBriefing(any());
    }

    @Test
    @DisplayName("generateDailyBriefing_외부서비스실패_FAILED상태")
    void generateDailyBriefing_외부서비스실패_FAILED상태() {
        // given
        given(briefingRepository.findByUserIdAndBriefingDate(USER_ID, LocalDate.now()))
                .willReturn(Optional.empty());

        Briefing savedBriefing = Briefing.builder()
                .userId(USER_ID)
                .briefingDate(LocalDate.now())
                .status(BriefingStatus.PENDING)
                .build();
        given(briefingRepository.save(any(Briefing.class))).willReturn(savedBriefing);

        given(integrationClient.collectWorkData())
                .willThrow(new RuntimeException("integration-service 연결 실패"));

        // when & then
        assertThatThrownBy(() -> briefingService.generateDailyBriefing(USER_ID))
                .isInstanceOf(BusinessException.class);

        assertThat(savedBriefing.getStatus()).isEqualTo(BriefingStatus.FAILED);
        assertThat(savedBriefing.getSummary()).contains("integration-service 연결 실패");
    }

    @Test
    @DisplayName("getBriefingHistory_정상호출_리스트반환")
    void getBriefingHistory_정상호출_리스트반환() {
        // given
        Briefing briefing1 = Briefing.builder()
                .userId(USER_ID)
                .briefingDate(LocalDate.now())
                .status(BriefingStatus.COMPLETED)
                .build();
        briefing1.complete("요약1", "내용1", List.of("포인트1"), List.of("액션1"), 1, 2, 3);

        Briefing briefing2 = Briefing.builder()
                .userId(USER_ID)
                .briefingDate(LocalDate.now().minusDays(1))
                .status(BriefingStatus.COMPLETED)
                .build();
        briefing2.complete("요약2", "내용2", List.of("포인트2"), List.of("액션2"), 4, 5, 6);

        given(briefingRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .willReturn(List.of(briefing1, briefing2));

        // when
        List<BriefingListResponse> history = briefingService.getBriefingHistory(USER_ID);

        // then
        assertThat(history).hasSize(2);
        assertThat(history.get(0).summary()).isEqualTo("요약1");
        assertThat(history.get(1).summary()).isEqualTo("요약2");
        assertThat(history.get(0).status()).isEqualTo(BriefingStatus.COMPLETED);
    }

    @Test
    @DisplayName("getBriefing_존재하지않는브리핑_예외발생")
    void getBriefing_존재하지않는브리핑_예외발생() {
        // given
        given(briefingRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> briefingService.getBriefing(USER_ID, 999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getTodayBriefing_브리핑없음_빈Optional반환")
    void getTodayBriefing_브리핑없음_빈Optional반환() {
        // given
        given(briefingRepository.findByUserIdAndBriefingDate(USER_ID, LocalDate.now()))
                .willReturn(Optional.empty());

        // when
        Optional<BriefingResponse> result = briefingService.getTodayBriefing(USER_ID);

        // then
        assertThat(result).isEmpty();
    }
}
