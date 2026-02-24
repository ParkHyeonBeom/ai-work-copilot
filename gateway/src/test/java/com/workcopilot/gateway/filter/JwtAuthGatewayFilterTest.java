package com.workcopilot.gateway.filter;

import com.workcopilot.gateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("JwtAuthGatewayFilter 단위 테스트")
@ExtendWith(MockitoExtension.class)
class JwtAuthGatewayFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain chain;

    private JwtAuthGatewayFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthGatewayFilter(jwtUtil);
    }

    @Test
    @DisplayName("filter_유효한Bearer토큰_X-User-Id헤더추가후통과")
    void filter_유효한Bearer토큰_XUserIdHeaderAdded() {
        // given
        String token = "valid.jwt.token";
        Long userId = 42L;
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(jwtUtil.validateToken(token)).willReturn(true);
        given(jwtUtil.getUserIdFromToken(token)).willReturn(userId);
        given(chain.filter(any())).willReturn(Mono.empty());

        // when
        Mono<Void> result = filter.filter(exchange, chain);

        // then
        StepVerifier.create(result).verifyComplete();
        verify(jwtUtil).validateToken(token);
        verify(jwtUtil).getUserIdFromToken(token);
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter_토큰없음_401반환")
    void filter_토큰없음_401반환() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/me")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = filter.filter(exchange, chain);

        // then
        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter_잘못된토큰_401반환")
    void filter_잘못된토큰_401반환() {
        // given
        String token = "invalid.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(jwtUtil.validateToken(token)).willReturn(false);

        // when
        Mono<Void> result = filter.filter(exchange, chain);

        // then
        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter_Bearer접두사없음_401반환")
    void filter_Bearer접두사없음_401반환() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic some-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = filter.filter(exchange, chain);

        // then
        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter_공개경로_인증없이통과")
    void filter_공개경로_인증없이통과() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(chain.filter(any())).willReturn(Mono.empty());

        // when
        Mono<Void> result = filter.filter(exchange, chain);

        // then
        StepVerifier.create(result).verifyComplete();
        verify(jwtUtil, never()).validateToken(any());
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter_OAuth2경로_인증없이통과")
    void filter_OAuth2경로_인증없이통과() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/oauth2/authorization/google")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(chain.filter(any())).willReturn(Mono.empty());

        // when
        Mono<Void> result = filter.filter(exchange, chain);

        // then
        StepVerifier.create(result).verifyComplete();
        verify(jwtUtil, never()).validateToken(any());
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter_OAuth2콜백경로_인증없이통과")
    void filter_OAuth2콜백경로_인증없이통과() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/login/oauth2/code/google")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(chain.filter(any())).willReturn(Mono.empty());

        // when
        Mono<Void> result = filter.filter(exchange, chain);

        // then
        StepVerifier.create(result).verifyComplete();
        verify(jwtUtil, never()).validateToken(any());
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter_actuatorHealth경로_인증없이통과")
    void filter_actuatorHealth경로_인증없이통과() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/actuator/health")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(chain.filter(any())).willReturn(Mono.empty());

        // when
        Mono<Void> result = filter.filter(exchange, chain);

        // then
        StepVerifier.create(result).verifyComplete();
        verify(jwtUtil, never()).validateToken(any());
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter_OPTIONS요청_인증없이통과")
    void filter_OPTIONS요청_인증없이통과() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .options("/api/users/me")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(chain.filter(any())).willReturn(Mono.empty());

        // when
        Mono<Void> result = filter.filter(exchange, chain);

        // then
        StepVerifier.create(result).verifyComplete();
        verify(jwtUtil, never()).validateToken(any());
        verify(chain).filter(any());
    }
}
