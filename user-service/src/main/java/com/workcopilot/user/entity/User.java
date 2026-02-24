package com.workcopilot.user.entity;

import com.workcopilot.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Builder.Default
    private boolean onboardingCompleted = false;

    @Convert(converter = UserSettingsConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private UserSettings settings = UserSettings.defaults();

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
}
