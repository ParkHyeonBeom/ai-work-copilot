package com.workcopilot.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebFlux 관련 설정.
 * GlobalErrorWebExceptionHandler에서 필요한 WebProperties 빈을 등록한다.
 * Spring Boot가 자동으로 등록하지 않는 경우를 대비한 fallback.
 */
@Configuration
public class WebConfig {

    @Bean
    @ConditionalOnMissingBean
    public WebProperties webProperties() {
        return new WebProperties();
    }
}
