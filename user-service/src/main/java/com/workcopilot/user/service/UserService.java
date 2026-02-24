package com.workcopilot.user.service;

import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.user.dto.GoogleTokenResponse;
import com.workcopilot.user.dto.OnboardingRequest;
import com.workcopilot.user.dto.UpdateSettingsRequest;
import com.workcopilot.user.dto.UserResponse;
import com.workcopilot.user.entity.User;
import com.workcopilot.user.entity.UserSettings;
import com.workcopilot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getMe(Long userId) {
        User user = findUserById(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateSettings(Long userId, UpdateSettingsRequest request) {
        User user = findUserById(userId);

        UserSettings settings = UserSettings.builder()
                .monitoredCalendarIds(request.monitoredCalendarIds())
                .monitoredDriveFolderIds(request.monitoredDriveFolderIds())
                .importantDomains(request.importantDomains())
                .excludeLabels(request.excludeLabels())
                .workStartTime(request.workStartTime())
                .workEndTime(request.workEndTime())
                .language(request.language())
                .timezone(request.timezone())
                .build();

        user.updateSettings(settings);
        userRepository.save(user);

        log.info("사용자 설정 변경: userId={}", userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse completeOnboarding(Long userId, OnboardingRequest request) {
        User user = findUserById(userId);

        UserSettings settings = UserSettings.builder()
                .monitoredCalendarIds(request.monitoredCalendarIds())
                .monitoredDriveFolderIds(request.monitoredDriveFolderIds())
                .importantDomains(request.importantDomains())
                .excludeLabels(user.getSettings().getExcludeLabels())
                .workStartTime(request.workStartTime())
                .workEndTime(request.workEndTime())
                .language(user.getSettings().getLanguage())
                .timezone(request.timezone())
                .build();

        user.updateSettings(settings);
        user.completeOnboarding();
        userRepository.save(user);

        log.info("온보딩 완료: userId={}", userId);
        return UserResponse.from(user);
    }

    public GoogleTokenResponse getGoogleToken(Long userId) {
        User user = findUserById(userId);
        return GoogleTokenResponse.from(user);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
