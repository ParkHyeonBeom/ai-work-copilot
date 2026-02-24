package com.workcopilot.user.service;

import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.user.dto.OnboardingRequest;
import com.workcopilot.user.dto.UpdateSettingsRequest;
import com.workcopilot.user.dto.UserResponse;
import com.workcopilot.user.entity.Role;
import com.workcopilot.user.entity.User;
import com.workcopilot.user.entity.UserSettings;
import com.workcopilot.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User createTestUser() {
        User user = User.builder()
                .email("test@example.com")
                .name("테스트")
                .googleId("google-123")
                .role(Role.USER)
                .settings(UserSettings.defaults())
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    @Test
    void getMe_존재하는유저_유저정보반환() {
        User user = createTestUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.getMe(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.name()).isEqualTo("테스트");
    }

    @Test
    void getMe_없는유저_예외발생() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void updateSettings_유효한요청_설정변경() {
        User user = createTestUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willReturn(user);

        UpdateSettingsRequest request = new UpdateSettingsRequest(
                List.of("primary", "work"),
                List.of("root"),
                List.of("company.com"),
                List.of("PROMOTIONS"),
                "08:00", "17:00", "ko", "Asia/Seoul"
        );

        UserResponse response = userService.updateSettings(1L, request);

        assertThat(response.settings().getWorkStartTime()).isEqualTo("08:00");
        assertThat(response.settings().getMonitoredCalendarIds()).containsExactly("primary", "work");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void completeOnboarding_유효한요청_온보딩완료() {
        User user = createTestUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willReturn(user);

        OnboardingRequest request = new OnboardingRequest(
                List.of("primary"),
                List.of("root"),
                List.of("important.com"),
                "09:00", "18:00", "Asia/Seoul"
        );

        UserResponse response = userService.completeOnboarding(1L, request);

        assertThat(response.onboardingCompleted()).isTrue();
        verify(userRepository).save(any(User.class));
    }
}
