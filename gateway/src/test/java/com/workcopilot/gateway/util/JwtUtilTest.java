package com.workcopilot.gateway.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil 단위 테스트")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private SecretKey signingKey;

    private static final String SECRET = "dev-secret-key-for-local-testing-only-change-in-production-minimum-256-bits";

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        // @Value와 @PostConstruct를 수동으로 설정
        Field secretField = JwtUtil.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, SECRET);

        Field signingKeyField = JwtUtil.class.getDeclaredField("signingKey");
        signingKeyField.setAccessible(true);
        signingKeyField.set(jwtUtil, signingKey);
    }

    @Test
    @DisplayName("validateToken_유효한토큰_true반환")
    void validateToken_유효한토큰_true반환() {
        // given
        String token = createToken(1L, 60_000L); // 1분 후 만료

        // when
        boolean result = jwtUtil.validateToken(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validateToken_만료된토큰_false반환")
    void validateToken_만료된토큰_false반환() {
        // given
        String token = createToken(1L, -1_000L); // 이미 만료됨

        // when
        boolean result = jwtUtil.validateToken(token);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateToken_잘못된토큰_false반환")
    void validateToken_잘못된토큰_false반환() {
        // given
        String token = "invalid.jwt.token";

        // when
        boolean result = jwtUtil.validateToken(token);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateToken_다른키로서명된토큰_false반환")
    void validateToken_다른키로서명된토큰_false반환() {
        // given
        SecretKey differentKey = Keys.hmacShaKeyFor(
                "another-secret-key-that-is-long-enough-for-hmac-sha256-algorithm-minimum".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(differentKey)
                .compact();

        // when
        boolean result = jwtUtil.validateToken(token);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateToken_null토큰_false반환")
    void validateToken_null토큰_false반환() {
        // when
        boolean result = jwtUtil.validateToken(null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateToken_빈문자열토큰_false반환")
    void validateToken_빈문자열토큰_false반환() {
        // when
        boolean result = jwtUtil.validateToken("");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getUserIdFromToken_유효한토큰_userId반환")
    void getUserIdFromToken_유효한토큰_userId반환() {
        // given
        Long expectedUserId = 42L;
        String token = createToken(expectedUserId, 60_000L);

        // when
        Long userId = jwtUtil.getUserIdFromToken(token);

        // then
        assertThat(userId).isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("getUserIdFromToken_만료된토큰_예외발생")
    void getUserIdFromToken_만료된토큰_예외발생() {
        // given
        String token = createToken(1L, -1_000L);

        // when & then
        assertThatThrownBy(() -> jwtUtil.getUserIdFromToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("getUserIdFromToken_잘못된토큰_예외발생")
    void getUserIdFromToken_잘못된토큰_예외발생() {
        // given
        String token = "invalid.jwt.token";

        // when & then
        assertThatThrownBy(() -> jwtUtil.getUserIdFromToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("getUserIdFromToken_다양한userId_정확히추출")
    void getUserIdFromToken_다양한userId_정확히추출() {
        // given
        long[] userIds = {1L, 100L, 999999L, Long.MAX_VALUE};

        for (long expectedId : userIds) {
            String token = createToken(expectedId, 60_000L);

            // when
            Long actualId = jwtUtil.getUserIdFromToken(token);

            // then
            assertThat(actualId).isEqualTo(expectedId);
        }
    }

    /**
     * 테스트용 JWT 토큰을 생성한다.
     *
     * @param userId         사용자 ID (subject에 설정)
     * @param validityMillis 만료 시간(밀리초). 음수이면 이미 만료된 토큰 생성
     * @return JWT 토큰 문자열
     */
    private String createToken(Long userId, long validityMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validityMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }
}
