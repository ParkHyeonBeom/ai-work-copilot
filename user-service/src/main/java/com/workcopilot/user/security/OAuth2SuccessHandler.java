package com.workcopilot.user.security;

import com.workcopilot.user.entity.Role;
import com.workcopilot.user.entity.User;
import com.workcopilot.user.entity.UserSettings;
import com.workcopilot.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> createUser(googleId, email, name, picture));

        user.updateProfile(name, picture);
        userRepository.save(user);

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("OAuth2 로그인 성공: userId={}, email={}", user.getId(), user.getEmail());

        String redirectUrl = String.format("%s/oauth/callback?accessToken=%s&refreshToken=%s",
                frontendUrl, accessToken, refreshToken);
        response.sendRedirect(redirectUrl);
    }

    private User createUser(String googleId, String email, String name, String picture) {
        log.info("신규 사용자 생성: email={}", email);
        return userRepository.save(User.builder()
                .googleId(googleId)
                .email(email)
                .name(name)
                .profileImageUrl(picture)
                .role(Role.USER)
                .settings(UserSettings.defaults())
                .build());
    }
}
