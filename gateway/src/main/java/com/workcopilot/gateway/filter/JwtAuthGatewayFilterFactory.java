package com.workcopilot.gateway.filter;

import com.workcopilot.gateway.util.JwtUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Route 레벨에서 JWT 인증 필터를 적용하기 위한 GatewayFilterFactory.
 *
 * application.yml에서 다음과 같이 사용할 수 있다:
 * <pre>
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: some-service
 *           uri: http://localhost:8081
 *           predicates:
 *             - Path=/api/some/**
 *           filters:
 *             - name: JwtAuth
 *               args:
 *                 excludedPaths: /api/some/public, /api/some/health
 * </pre>
 */
@Slf4j
@Component
public class JwtAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {

    private final JwtUtil jwtUtil;

    public JwtAuthGatewayFilterFactory(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new JwtAuthGatewayFilter(jwtUtil);
    }

    @Override
    public String name() {
        return "JwtAuth";
    }

    /**
     * 필터 설정 클래스.
     * excludedPaths를 통해 특정 경로를 JWT 검증에서 제외할 수 있다.
     */
    @Getter
    @Setter
    public static class Config {
        /** JWT 검증을 건너뛸 경로 목록 (Ant 패턴 지원) */
        private List<String> excludedPaths = List.of();
    }
}
