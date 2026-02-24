package com.workcopilot.user.dto;

import com.workcopilot.user.entity.User;

import java.time.Instant;

public record GoogleTokenResponse(
        String accessToken,
        String refreshToken,
        Instant expiresAt
) {
    public static GoogleTokenResponse from(User user) {
        return new GoogleTokenResponse(
                user.getGoogleAccessToken(),
                user.getGoogleRefreshToken(),
                user.getGoogleTokenExpiresAt()
        );
    }
}
