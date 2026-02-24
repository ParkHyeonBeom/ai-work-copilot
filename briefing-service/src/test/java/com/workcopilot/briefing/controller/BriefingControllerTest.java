package com.workcopilot.briefing.controller;

import com.workcopilot.briefing.dto.BriefingListResponse;
import com.workcopilot.briefing.dto.BriefingResponse;
import com.workcopilot.briefing.entity.BriefingStatus;
import com.workcopilot.briefing.security.JwtProvider;
import com.workcopilot.briefing.service.BriefingService;
import com.workcopilot.briefing.service.BriefingStreamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BriefingController.class)
class BriefingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BriefingService briefingService;

    @MockBean
    private BriefingStreamService briefingStreamService;

    @MockBean
    private JwtProvider jwtProvider;

    private static final Long USER_ID = 1L;

    private UsernamePasswordAuthenticationToken createAuth() {
        return new UsernamePasswordAuthenticationToken(
                USER_ID, "test-token",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("generateDailyBriefing_인증된유저_정상응답")
    void generateDailyBriefing_인증된유저_정상응답() throws Exception {
        // given
        BriefingResponse response = new BriefingResponse(
                1L, USER_ID, LocalDate.now(), BriefingStatus.COMPLETED,
                "오늘의 요약", "상세 내용",
                List.of("포인트1"), List.of("액션1"),
                3, 5, 2,
                LocalDateTime.now(), LocalDateTime.now()
        );
        given(briefingService.generateDailyBriefing(USER_ID)).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/briefings/daily")
                        .with(authentication(createAuth()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary").value("오늘의 요약"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("getBriefingHistory_인증된유저_리스트반환")
    void getBriefingHistory_인증된유저_리스트반환() throws Exception {
        // given
        List<BriefingListResponse> history = List.of(
                new BriefingListResponse(1L, LocalDate.now(), BriefingStatus.COMPLETED,
                        "요약1", LocalDateTime.now()),
                new BriefingListResponse(2L, LocalDate.now().minusDays(1), BriefingStatus.COMPLETED,
                        "요약2", LocalDateTime.now())
        );
        given(briefingService.getBriefingHistory(USER_ID)).willReturn(history);

        // when & then
        mockMvc.perform(get("/api/briefings/history")
                        .with(authentication(createAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("getTodayBriefing_브리핑없음_빈데이터응답")
    void getTodayBriefing_브리핑없음_빈데이터응답() throws Exception {
        // given
        given(briefingService.getTodayBriefing(USER_ID)).willReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/briefings/today")
                        .with(authentication(createAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("오늘의 브리핑이 아직 생성되지 않았습니다."));
    }

    @Test
    @DisplayName("generateDailyBriefing_미인증_403응답")
    void generateDailyBriefing_미인증_403응답() throws Exception {
        // when & then - 기본 Spring Security: 미인증 + CSRF 없음 → 403
        mockMvc.perform(post("/api/briefings/daily"))
                .andExpect(status().isForbidden());
    }
}
