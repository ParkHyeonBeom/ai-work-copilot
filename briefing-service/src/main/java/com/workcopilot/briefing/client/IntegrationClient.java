package com.workcopilot.briefing.client;

import com.workcopilot.briefing.dto.WorkDataDto;
import com.workcopilot.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationClient {

    private final RestTemplate restTemplate;

    @Value("${integration-service.url}")
    private String integrationServiceUrl;

    public WorkDataDto collectWorkData() {
        log.info("integration-service 업무 데이터 수집 요청");

        ResponseEntity<ApiResponse<WorkDataDto>> response = restTemplate.exchange(
                integrationServiceUrl + "/api/integrations/data/collect",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        ApiResponse<WorkDataDto> body = response.getBody();
        if (body == null || !body.isSuccess()) {
            throw new RuntimeException("integration-service 데이터 수집 실패");
        }

        log.info("업무 데이터 수집 완료");
        return body.getData();
    }
}
