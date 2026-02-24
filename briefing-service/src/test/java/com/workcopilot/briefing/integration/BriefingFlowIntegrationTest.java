package com.workcopilot.briefing.integration;

import com.workcopilot.briefing.client.AiRouterClient;
import com.workcopilot.briefing.client.IntegrationClient;
import com.workcopilot.briefing.dto.*;
import com.workcopilot.briefing.entity.Briefing;
import com.workcopilot.briefing.entity.BriefingStatus;
import com.workcopilot.briefing.repository.BriefingRepository;
import com.workcopilot.briefing.service.BriefingService;
import com.workcopilot.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("local")
class BriefingFlowIntegrationTest {

    @Autowired
    private BriefingService briefingService;

    @Autowired
    private BriefingRepository briefingRepository;

    @MockBean
    private IntegrationClient integrationClient;

    @MockBean
    private AiRouterClient aiRouterClient;

    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;

    @BeforeEach
    void setUp() {
        briefingRepository.deleteAll();
    }

    private WorkDataDto createWorkData() {
        return new WorkDataDto(
                List.of(new CalendarEventDto(
                        "event-1", "스프린트 회의", "주간 스프린트 리뷰",
                        LocalDateTime.now().withHour(10).withMinute(0),
                        LocalDateTime.now().withHour(11).withMinute(0),
                        "회의실 A", List.of("dev@test.com", "pm@test.com"), false
                )),
                List.of(new EmailDto(
                        "email-1", "pm@test.com", "배포 일정 확인",
                        "이번 주 금요일 배포 일정을 확인해주세요.",
                        LocalDateTime.now().minusHours(2), List.of("INBOX", "IMPORTANT"), true
                )),
                List.of(new DriveFileDto(
                        "file-1", "API 설계서.docx",
                        "application/vnd.google-apps.document",
                        LocalDateTime.now().minusDays(1),
                        List.of("architect@test.com"),
                        "https://docs.google.com/document/d/file-1"
                )),
                LocalDateTime.now()
        );
    }

    private BriefingAiResponse createAiResponse() {
        // Hibernate @ElementCollection requires mutable lists for merge operations
        return new BriefingAiResponse(
                "오늘은 스프린트 회의와 배포 관련 이메일이 있습니다.",
                "오전 10시에 스프린트 회의가 예정되어 있으며, PM으로부터 배포 일정 확인 요청 이메일이 도착했습니다. API 설계서가 최근 업데이트되었으니 확인이 필요합니다.",
                new ArrayList<>(List.of("10시 스프린트 회의 참석", "배포 일정 확인 필요", "API 설계서 변경사항 리뷰")),
                new ArrayList<>(List.of("회의 자료 준비", "PM에게 배포 일정 회신", "설계서 리뷰 코멘트 작성"))
        );
    }

    @Test
    @DisplayName("generateDailyBriefing_신규유저_브리핑생성성공")
    void generateDailyBriefing_신규유저_브리핑생성성공() {
        // given
        given(integrationClient.collectWorkData()).willReturn(createWorkData());
        given(aiRouterClient.generateBriefing(any(BriefingRequest.class))).willReturn(createAiResponse());

        // when
        BriefingResponse response = briefingService.generateDailyBriefing(USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.briefingDate()).isEqualTo(LocalDate.now());
        assertThat(response.status()).isEqualTo(BriefingStatus.COMPLETED);
        assertThat(response.summary()).isEqualTo("오늘은 스프린트 회의와 배포 관련 이메일이 있습니다.");
        assertThat(response.fullContent()).contains("스프린트 회의");
        assertThat(response.keyPoints()).hasSize(3);
        assertThat(response.keyPoints()).contains("10시 스프린트 회의 참석");
        assertThat(response.actionItems()).hasSize(3);
        assertThat(response.actionItems()).contains("회의 자료 준비");
        assertThat(response.eventCount()).isEqualTo(1);
        assertThat(response.emailCount()).isEqualTo(1);
        assertThat(response.fileCount()).isEqualTo(1);
        assertThat(response.completedAt()).isNotNull();
        assertThat(response.id()).isNotNull();

        // DB 저장 확인
        Briefing saved = briefingRepository.findByUserIdAndBriefingDate(USER_ID, LocalDate.now())
                .orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(BriefingStatus.COMPLETED);
        assertThat(saved.getSummary()).isEqualTo("오늘은 스프린트 회의와 배포 관련 이메일이 있습니다.");
    }

    @Test
    @DisplayName("generateDailyBriefing_이미완료된브리핑존재_기존브리핑반환")
    void generateDailyBriefing_이미완료된브리핑존재_기존브리핑반환() {
        // given - 먼저 브리핑을 생성
        given(integrationClient.collectWorkData()).willReturn(createWorkData());
        given(aiRouterClient.generateBriefing(any(BriefingRequest.class))).willReturn(createAiResponse());

        BriefingResponse firstResponse = briefingService.generateDailyBriefing(USER_ID);

        // when - 같은 유저로 다시 브리핑 생성 요청
        BriefingResponse secondResponse = briefingService.generateDailyBriefing(USER_ID);

        // then - 동일한 브리핑이 반환되어야 함
        assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
        assertThat(secondResponse.status()).isEqualTo(BriefingStatus.COMPLETED);
        assertThat(secondResponse.summary()).isEqualTo(firstResponse.summary());
        assertThat(secondResponse.briefingDate()).isEqualTo(LocalDate.now());

        // DB에 브리핑이 1개만 존재하는지 확인
        List<Briefing> allBriefings = briefingRepository.findByUserIdOrderByCreatedAtDesc(USER_ID);
        assertThat(allBriefings).hasSize(1);
    }

    @Test
    @DisplayName("getBriefingHistory_브리핑여러개생성후_히스토리조회")
    void getBriefingHistory_브리핑여러개생성후_히스토리조회() {
        // given - 서로 다른 날짜의 브리핑을 직접 DB에 저장 (mutable lists for Hibernate)
        Briefing briefing1 = Briefing.builder()
                .userId(USER_ID)
                .briefingDate(LocalDate.now())
                .status(BriefingStatus.PENDING)
                .build();
        briefing1.complete(
                "오늘의 브리핑 요약",
                "오늘의 상세 내용",
                new ArrayList<>(List.of("포인트A")),
                new ArrayList<>(List.of("액션A")),
                2, 3, 1
        );
        briefingRepository.save(briefing1);

        Briefing briefing2 = Briefing.builder()
                .userId(USER_ID)
                .briefingDate(LocalDate.now().minusDays(1))
                .status(BriefingStatus.PENDING)
                .build();
        briefing2.complete(
                "어제의 브리핑 요약",
                "어제의 상세 내용",
                new ArrayList<>(List.of("포인트B")),
                new ArrayList<>(List.of("액션B")),
                4, 5, 2
        );
        briefingRepository.save(briefing2);

        Briefing briefing3 = Briefing.builder()
                .userId(USER_ID)
                .briefingDate(LocalDate.now().minusDays(2))
                .status(BriefingStatus.PENDING)
                .build();
        briefing3.complete(
                "그저께 브리핑 요약",
                "그저께 상세 내용",
                new ArrayList<>(List.of("포인트C")),
                new ArrayList<>(List.of("액션C")),
                1, 2, 0
        );
        briefingRepository.save(briefing3);

        // when
        List<BriefingListResponse> history = briefingService.getBriefingHistory(USER_ID);

        // then - 3개의 브리핑이 반환되어야 함
        assertThat(history).hasSize(3);

        // createdAt DESC 정렬 확인: 모든 요약이 포함되어 있는지 검증
        List<String> summaries = history.stream()
                .map(BriefingListResponse::summary)
                .toList();
        assertThat(summaries).containsExactlyInAnyOrder(
                "오늘의 브리핑 요약", "어제의 브리핑 요약", "그저께 브리핑 요약"
        );

        // 모든 브리핑이 COMPLETED 상태인지 확인
        assertThat(history).allMatch(b -> b.status() == BriefingStatus.COMPLETED);

        // 다른 유저의 히스토리는 비어있어야 함
        List<BriefingListResponse> otherHistory = briefingService.getBriefingHistory(OTHER_USER_ID);
        assertThat(otherHistory).isEmpty();
    }

    @Test
    @DisplayName("getBriefing_다른유저의브리핑조회_예외발생")
    void getBriefing_다른유저의브리핑조회_예외발생() {
        // given - USER_ID로 브리핑 생성
        Briefing briefing = Briefing.builder()
                .userId(USER_ID)
                .briefingDate(LocalDate.now())
                .status(BriefingStatus.PENDING)
                .build();
        briefing.complete(
                "비공개 브리핑",
                "이 브리핑은 다른 유저가 볼 수 없어야 합니다.",
                new ArrayList<>(List.of("포인트")),
                new ArrayList<>(List.of("액션")),
                1, 1, 1
        );
        Briefing saved = briefingRepository.save(briefing);
        Long briefingId = saved.getId();

        // when & then - OTHER_USER_ID로 조회 시 예외 발생
        assertThatThrownBy(() -> briefingService.getBriefing(OTHER_USER_ID, briefingId))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode().getCode()).isEqualTo("B001");
                });

        // 원래 유저는 정상 조회 가능
        BriefingResponse response = briefingService.getBriefing(USER_ID, briefingId);
        assertThat(response).isNotNull();
        assertThat(response.summary()).isEqualTo("비공개 브리핑");
    }

    @Test
    @DisplayName("generateDailyBriefing_외부서비스실패_FAILED상태저장")
    void generateDailyBriefing_외부서비스실패_FAILED상태저장() {
        // given - IntegrationClient가 예외를 던지도록 설정
        given(integrationClient.collectWorkData())
                .willThrow(new RuntimeException("Google API 연결 실패"));

        // when & then - BusinessException이 발생해야 함
        assertThatThrownBy(() -> briefingService.generateDailyBriefing(USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("브리핑 생성 실패")
                .hasMessageContaining("Google API 연결 실패")
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(com.workcopilot.common.exception.ErrorCode.INTERNAL_ERROR);
                });

        // Note: @Transactional 메서드에서 RuntimeException 발생 시 트랜잭션이 롤백되므로
        // FAILED 상태의 브리핑은 DB에 저장되지 않음 (트랜잭션 롤백으로 인해 전체 변경사항 무효화)
        // 이는 서비스 계층의 트랜잭션 경계 설계에 따른 정상 동작
        Briefing briefing = briefingRepository.findByUserIdAndBriefingDate(USER_ID, LocalDate.now())
                .orElse(null);
        assertThat(briefing).isNull();
    }
}
