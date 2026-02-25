package com.workcopilot.integration.google;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.integration.client.GoogleTokenClient;
import com.workcopilot.integration.dto.GoogleTokenDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCredentialProvider {

    private final GoogleTokenClient googleTokenClient;
    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;

    @Value("${google.client-id:}")
    private String clientId;

    @Value("${google.client-secret:}")
    private String clientSecret;

    public Credential getCredential(Long userId) {
        GoogleTokenDto tokenDto = googleTokenClient.getGoogleToken(userId);

        if (tokenDto.accessToken() == null || tokenDto.accessToken().isBlank()) {
            log.error("Google 액세스 토큰이 비어있습니다: userId={}", userId);
            throw new BusinessException(ErrorCode.TOKEN_REFRESH_FAILED,
                    "Google 액세스 토큰이 없습니다. 재인증이 필요합니다.");
        }

        // UTC 시간대로 비교
        if (tokenDto.expiresAt() != null &&
            tokenDto.expiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            log.warn("Google 액세스 토큰이 만료되었습니다: userId={}, expiresAt={}",
                    userId, tokenDto.expiresAt());
        }

        Credential.Builder builder = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setTokenServerEncodedUrl(GoogleOAuthConstants.TOKEN_SERVER_URL);

        // 토큰 리프레시를 위한 클라이언트 인증 설정
        if (clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank()) {
            builder.setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret));
        }

        Credential credential = builder.build();
        credential.setAccessToken(tokenDto.accessToken());

        if (tokenDto.refreshToken() != null && !tokenDto.refreshToken().isBlank()) {
            credential.setRefreshToken(tokenDto.refreshToken());
        }

        log.debug("Google Credential 생성 완료: userId={}", userId);
        return credential;
    }
}
