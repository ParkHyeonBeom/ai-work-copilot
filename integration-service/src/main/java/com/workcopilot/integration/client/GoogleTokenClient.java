package com.workcopilot.integration.client;

import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.integration.dto.GoogleTokenDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenClient {

    private final RestTemplate restTemplate;

    @Value("${user-service.url}")
    private String userServiceUrl;

    public GoogleTokenDto getGoogleToken(Long userId) {
        String url = userServiceUrl + "/api/internal/users/" + userId + "/google-token";

        try {
            log.info("user-service에서 Google 토큰 조회: userId={}", userId);

            ResponseEntity<ApiResponse<GoogleTokenDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            ApiResponse<GoogleTokenDto> body = response.getBody();
            if (body == null || !body.isSuccess() || body.getData() == null) {
                log.error("Google 토큰 조회 실패: userId={}", userId);
                throw new BusinessException(ErrorCode.TOKEN_REFRESH_FAILED,
                        "사용자의 Google 토큰을 조회할 수 없습니다.");
            }

            log.info("Google 토큰 조회 성공: userId={}", userId);
            return body.getData();

        } catch (RestClientException e) {
            log.error("user-service 통신 실패: userId={}, error={}", userId, e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR,
                    "user-service와의 통신에 실패했습니다: " + e.getMessage());
        }
    }
}
