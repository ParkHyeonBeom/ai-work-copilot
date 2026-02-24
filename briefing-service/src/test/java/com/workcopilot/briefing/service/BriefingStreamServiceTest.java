package com.workcopilot.briefing.service;

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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BriefingStreamServiceTest {

    @Mock
    private BriefingRepository briefingRepository;

    @InjectMocks
    private BriefingStreamService briefingStreamService;

    private static final Long USER_ID = 1L;
    private static final Long BRIEFING_ID = 10L;

    @Test
    @DisplayName("streamBriefing_완료된브리핑_청크스트리밍")
    void streamBriefing_완료된브리핑_청크스트리밍() {
        // given
        Briefing briefing = Briefing.builder()
                .userId(USER_ID)
                .briefingDate(LocalDate.now())
                .status(BriefingStatus.COMPLETED)
                .build();
        briefing.complete(
                "요약",
                "오늘의 브리핑 상세 내용입니다. 여러 청크로 나뉘어 스트리밍됩니다.",
                List.of("포인트1", "포인트2"),
                List.of("액션1"),
                3, 5, 2
        );

        given(briefingRepository.findById(BRIEFING_ID)).willReturn(Optional.of(briefing));

        // when
        SseEmitter emitter = briefingStreamService.streamBriefing(USER_ID, BRIEFING_ID);

        // then
        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(5 * 60 * 1000L);
    }

    @Test
    @DisplayName("streamBriefing_존재하지않는브리핑_예외발생")
    void streamBriefing_존재하지않는브리핑_예외발생() {
        // given
        given(briefingRepository.findById(BRIEFING_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> briefingStreamService.streamBriefing(USER_ID, BRIEFING_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("streamBriefing_다른유저의브리핑_예외발생")
    void streamBriefing_다른유저의브리핑_예외발생() {
        // given
        Briefing briefing = Briefing.builder()
                .userId(999L) // 다른 유저
                .briefingDate(LocalDate.now())
                .status(BriefingStatus.COMPLETED)
                .build();

        given(briefingRepository.findById(BRIEFING_ID)).willReturn(Optional.of(briefing));

        // when & then
        assertThatThrownBy(() -> briefingStreamService.streamBriefing(USER_ID, BRIEFING_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("streamBriefing_생성중브리핑_에미터반환")
    void streamBriefing_생성중브리핑_에미터반환() {
        // given
        Briefing briefing = Briefing.builder()
                .userId(USER_ID)
                .briefingDate(LocalDate.now())
                .status(BriefingStatus.GENERATING)
                .build();

        given(briefingRepository.findById(BRIEFING_ID)).willReturn(Optional.of(briefing));

        // when
        SseEmitter emitter = briefingStreamService.streamBriefing(USER_ID, BRIEFING_ID);

        // then
        assertThat(emitter).isNotNull();
    }
}
