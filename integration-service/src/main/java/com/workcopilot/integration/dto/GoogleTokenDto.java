package com.workcopilot.integration.dto;

import java.time.LocalDateTime;

public record GoogleTokenDto(
        String accessToken,
        String refreshToken,
        LocalDateTime expiresAt
) {
}
