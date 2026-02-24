package com.workcopilot.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workcopilot.user.dto.UpdateSettingsRequest;
import com.workcopilot.user.dto.UserResponse;
import com.workcopilot.user.entity.Role;
import com.workcopilot.user.entity.UserSettings;
import com.workcopilot.user.repository.UserRepository;
import com.workcopilot.user.security.JwtAuthenticationFilter;
import com.workcopilot.user.security.JwtProvider;
import com.workcopilot.user.security.OAuth2SuccessHandler;
import com.workcopilot.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws Exception {
        // Mock 필터가 요청을 다음 필터로 전달하도록 설정
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    private UserResponse createTestResponse() {
        return new UserResponse(
                1L, "test@example.com", "테스트", null,
                Role.USER, false, UserSettings.defaults(),
                LocalDateTime.now()
        );
    }

    private static UsernamePasswordAuthenticationToken testAuth() {
        return new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void getMe_인증된사용자_유저정보반환() throws Exception {
        given(userService.getMe(1L)).willReturn(createTestResponse());

        mockMvc.perform(get("/api/users/me")
                        .with(authentication(testAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    void getMe_미인증사용자_리다이렉트() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void updateSettings_유효한요청_설정변경() throws Exception {
        given(userService.updateSettings(any(), any())).willReturn(createTestResponse());

        UpdateSettingsRequest request = new UpdateSettingsRequest(
                List.of("primary"), List.of("root"),
                List.of("company.com"), List.of("PROMOTIONS"),
                "09:00", "18:00", "ko", "Asia/Seoul"
        );

        mockMvc.perform(put("/api/users/me/settings")
                        .with(authentication(testAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
