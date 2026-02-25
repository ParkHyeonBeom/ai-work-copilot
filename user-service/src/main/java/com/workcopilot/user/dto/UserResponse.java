package com.workcopilot.user.dto;

import com.workcopilot.user.entity.Role;
import com.workcopilot.user.entity.User;
import com.workcopilot.user.entity.UserSettings;
import com.workcopilot.user.entity.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String name,
        String profileImageUrl,
        Role role,
        UserStatus status,
        boolean onboardingCompleted,
        UserSettings settings,
        String position,
        String department,
        String internalPhone,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getStatus(),
                user.isOnboardingCompleted(),
                user.getSettings(),
                user.getPosition(),
                user.getDepartment(),
                user.getInternalPhone(),
                user.getCreatedAt()
        );
    }
}
