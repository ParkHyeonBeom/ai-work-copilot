package com.workcopilot.user.entity;

import com.workcopilot.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    private String profileImageUrl;

    @Column(unique = true)
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_APPROVAL;

    @Builder.Default
    private boolean onboardingCompleted = false;

    @Column(columnDefinition = "TEXT")
    private String googleAccessToken;

    @Column(columnDefinition = "TEXT")
    private String googleRefreshToken;

    private Instant googleTokenExpiresAt;

    private String position;

    private String department;

    private String internalPhone;

    @Convert(converter = UserSettingsConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private UserSettings settings = UserSettings.defaults();

    public void updateGoogleTokens(String accessToken, String refreshToken, Instant expiresAt) {
        this.googleAccessToken = accessToken;
        if (refreshToken != null) {
            this.googleRefreshToken = refreshToken;
        }
        this.googleTokenExpiresAt = expiresAt;
    }

    public void updateSettings(UserSettings settings) {
        this.settings = settings;
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    public void updateProfile(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public void applyAdminProfile(String position, String department, String internalPhone) {
        this.position = position;
        this.department = department;
        this.internalPhone = internalPhone;
    }

    public void approve() {
        this.status = UserStatus.EMAIL_VERIFICATION;
    }

    public void verify() {
        this.status = UserStatus.ACTIVE;
    }

    public void reject() {
        this.status = UserStatus.REJECTED;
    }
}
