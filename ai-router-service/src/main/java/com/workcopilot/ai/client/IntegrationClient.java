package com.workcopilot.ai.client;

import com.workcopilot.ai.dto.WorkDataDto;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class IntegrationClient {

    private final RestTemplate restTemplate;
    private final String integrationServiceUrl;

    public IntegrationClient(
            RestTemplate restTemplate,
            @Value("${integration-service.url}") String integrationServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.integrationServiceUrl = integrationServiceUrl;
    }

    /**
     * integration-service의 데이터 수집 API를 호출하여 업무 데이터를 조회한다.
     *
     * @param userId 사용자 ID
     * @param token  JWT 토큰 (Authorization 전달용)
     * @return 수집된 업무 데이터
     */
    public WorkDataDto collectWorkData(Long userId, String token) {
        String url = integrationServiceUrl + "/api/data/collect";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.info("integration-service 데이터 수집 요청: userId={}, url={}", userId, url);

            ResponseEntity<WorkDataDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            WorkDataDto workData = response.getBody();
            log.info("integration-service 데이터 수집 완료: userId={}, events={}, emails={}, files={}",
                    userId,
                    workData != null && workData.events() != null ? workData.events().size() : 0,
                    workData != null && workData.emails() != null ? workData.emails().size() : 0,
                    workData != null && workData.files() != null ? workData.files().size() : 0);

            return workData;
        } catch (RestClientException e) {
            log.error("integration-service 호출 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "integration-service 호출 실패: " + e.getMessage());
        }
    }
}
