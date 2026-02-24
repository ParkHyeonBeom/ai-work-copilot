package com.workcopilot.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Rate Limiting 설정.
 *
 * local 프로파일에서는 Rate Limiting을 적용하지 않는다.
 * prod 프로파일에서는 Redis 기반 Rate Limiting을 적용한다.
 *
 * <p>prod 환경에서 활성화할 설정 예시:
 * <pre>
 * {@code
 * @Bean
 * public KeyResolver userKeyResolver() {
 *     return exchange -> {
 *         String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
 *         return Mono.just(userId != null ? userId : exchange.getRequest()
 *                 .getRemoteAddress().getAddress().getHostAddress());
 *     };
 * }
 *
 * @Bean
 * public RedisRateLimiter redisRateLimiter() {
 *     // replenishRate: 초당 허용 요청 수
 *     // burstCapacity: 순간 최대 요청 수
 *     return new RedisRateLimiter(10, 20);
 * }
 * }
 * </pre>
 *
 * <p>application.yml route 설정에 다음을 추가:
 * <pre>
 * filters:
 *   - name: RequestRateLimiter
 *     args:
 *       redis-rate-limiter.replenishRate: 10
 *       redis-rate-limiter.burstCapacity: 20
 *       key-resolver: "#{@userKeyResolver}"
 * </pre>
 */
@Slf4j
@Configuration
public class RateLimitConfig {

    // local 프로파일에서는 Rate Limiting 없이 동작
    // prod 프로파일에서 Redis 기반 Rate Limiting 활성화 시 위 주석의 Bean을 등록한다
}
