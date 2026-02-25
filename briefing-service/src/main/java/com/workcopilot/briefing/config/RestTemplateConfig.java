package com.workcopilot.briefing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(jwtForwardingInterceptor()));
        return restTemplate;
    }

    private ClientHttpRequestInterceptor jwtForwardingInterceptor() {
        return (request, body, execution) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                Object credentials = authentication.getCredentials();
                log.info("JWT 포워딩 - principal: {}, credentialsType: {}",
                        authentication.getPrincipal(),
                        credentials != null ? credentials.getClass().getSimpleName() : "null");
                if (credentials instanceof String token && !token.isBlank()) {
                    request.getHeaders().setBearerAuth(token);
                    log.info("JWT 토큰 헤더 추가 완료 (길이: {})", token.length());
                } else {
                    log.warn("JWT 토큰이 credentials에 없음: credentialsType={}",
                            credentials != null ? credentials.getClass().getSimpleName() : "null");
                }
            } else {
                log.warn("SecurityContextHolder에 인증 정보 없음");
            }
            return execution.execute(request, body);
        };
    }
}
