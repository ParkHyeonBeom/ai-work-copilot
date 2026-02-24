package com.workcopilot.gateway.config;

import com.workcopilot.gateway.filter.JwtAuthGatewayFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway 필터 설정.
 * JwtAuthGatewayFilter를 GlobalFilter로 등록하여 모든 라우트에 적용한다.
 */
@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final JwtAuthGatewayFilter jwtAuthGatewayFilter;

    /**
     * JwtAuthGatewayFilter를 GlobalFilter로 래핑하여 등록한다.
     * 모든 라우트 요청에 대해 JWT 인증을 수행한다.
     */
    @Bean
    public GlobalFilter jwtAuthGlobalFilter() {
        return (exchange, chain) -> jwtAuthGatewayFilter.filter(exchange, chain);
    }
}
