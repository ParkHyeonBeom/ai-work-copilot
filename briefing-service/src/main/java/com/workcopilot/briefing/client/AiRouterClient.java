package com.workcopilot.briefing.client;

import com.workcopilot.briefing.dto.BriefingAiResponse;
import com.workcopilot.briefing.dto.BriefingRequest;
import com.workcopilot.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiRouterClient {

    private final RestTemplate restTemplate;

    @Value("${ai-router-service.url}")
    private String aiRouterServiceUrl;

    public BriefingAiResponse generateBriefing(BriefingRequest request) {
        log.info("ai-router-service 브리핑 생성 요청: userId={}", request.userId());

        HttpEntity<BriefingRequest> httpEntity = new HttpEntity<>(request);

        ResponseEntity<ApiResponse<BriefingAiResponse>> response = restTemplate.exchange(
                aiRouterServiceUrl + "/api/ai/briefing",
                HttpMethod.POST,
                httpEntity,
                new ParameterizedTypeReference<>() {}
        );

        ApiResponse<BriefingAiResponse> body = response.getBody();
        if (body == null || !body.isSuccess()) {
            throw new RuntimeException("ai-router-service 브리핑 생성 실패");
        }

        log.info("AI 브리핑 생성 완료: userId={}", request.userId());
        return body.getData();
    }
}
