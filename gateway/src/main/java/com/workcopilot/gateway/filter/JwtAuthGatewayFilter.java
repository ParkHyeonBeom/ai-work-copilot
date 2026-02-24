package com.workcopilot.gateway.filter;

import com.workcopilot.gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 인증 Gateway Filter.
 * Authorization 헤더에서 Bearer 토큰을 추출하여 검증하고,
 * 검증 성공 시 X-User-Id 헤더를 다운스트림 서비스로 전달한다.
 */
@Slf4j
@Component
public class JwtAuthGatewayFilter implements GatewayFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthGatewayFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** JWT 검증을 건너뛸 공개 경로 */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/actuator/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // CORS preflight 요청은 무조건 통과
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // API 경로가 아닌 요청은 프론트엔드이므로 JWT 검증 없이 통과
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        // 공개 API 경로는 JWT 검증 없이 통과
        if (isPublicPath(path)) {
            log.debug("공개 경로 접근 허용: {}", path);
            return chain.filter(exchange);
        }

        // Authorization 헤더에서 Bearer 토큰 추출
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("인증 토큰 없음 - path: {}", path);
            return onUnauthorized(exchange, "인증 토큰이 필요합니다.");
        }

        String token = authHeader.substring(7);

        // JWT 토큰 검증
        if (!jwtUtil.validateToken(token)) {
            log.warn("유효하지 않은 JWT 토큰 - path: {}", path);
            return onUnauthorized(exchange, "유효하지 않은 인증 토큰입니다.");
        }

        // userId 추출 후 X-User-Id 헤더에 추가
        Long userId = jwtUtil.getUserIdFromToken(token);
        log.debug("JWT 인증 성공 - userId: {}, path: {}", userId, path);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -1; // 높은 우선순위
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"success":false,"message":"%s","code":"UNAUTHORIZED"}""".formatted(message);

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}
