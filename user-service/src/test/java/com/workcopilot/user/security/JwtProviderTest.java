package com.workcopilot.user.security;

import com.workcopilot.user.entity.Role;
import com.workcopilot.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret",
                "test-secret-key-for-testing-only-minimum-256-bits-required-here");
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiry", 3600000L);
        ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpiry", 604800000L);
        jwtProvider.init();
    }

    private User createTestUser() {
        User user = User.builder()
                .email("test@example.com")
                .name("테스트")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    @Test
    void generateAccessToken_유효한유저_토큰생성() {
        User user = createTestUser();

        String token = jwtProvider.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    void generateRefreshToken_유효한유저_토큰생성() {
        User user = createTestUser();

        String token = jwtProvider.generateRefreshToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    void getUserIdFromToken_유효한토큰_userId반환() {
        User user = createTestUser();
        String token = jwtProvider.generateAccessToken(user);

        Long userId = jwtProvider.getUserIdFromToken(token);

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    void getEmailFromToken_유효한토큰_email반환() {
        User user = createTestUser();
        String token = jwtProvider.generateAccessToken(user);

        String email = jwtProvider.getEmailFromToken(token);

        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    void validateToken_만료된토큰_false() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret",
                "test-secret-key-for-testing-only-minimum-256-bits-required-here");
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiry", -1000L);
        ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpiry", -1000L);
        jwtProvider.init();

        User user = createTestUser();
        String token = jwtProvider.generateAccessToken(user);

        assertThat(jwtProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_잘못된토큰_false() {
        assertThat(jwtProvider.validateToken("invalid.token.here")).isFalse();
    }
}
