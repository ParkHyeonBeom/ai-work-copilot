package com.workcopilot.user.security;

import com.workcopilot.common.audit.AuditAction;
import com.workcopilot.common.audit.Audited;
import com.workcopilot.user.entity.Role;
import com.workcopilot.user.entity.User;
import com.workcopilot.user.entity.UserSettings;
import com.workcopilot.user.entity.UserStatus;
import com.workcopilot.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.admin-email:admin@workcopilot.com}")
    private String adminEmail;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        User user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> createUser(googleId, email, name, picture));

        // googleId가 없거나 다른 경우 업데이트
        if (user.getGoogleId() == null || !user.getGoogleId().equals(googleId)) {
            user.updateGoogleId(googleId);
        }

        user.updateProfile(name, picture);

        // Google OAuth2 토큰 저장
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
        if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
            String googleAccessToken = authorizedClient.getAccessToken().getTokenValue();
            String googleRefreshToken = authorizedClient.getRefreshToken() != null
                    ? authorizedClient.getRefreshToken().getTokenValue() : null;
            user.updateGoogleTokens(googleAccessToken, googleRefreshToken,
                    authorizedClient.getAccessToken().getExpiresAt());
            log.info("Google 토큰 저장: userId={}, hasRefreshToken={}", user.getId(), googleRefreshToken != null);
        }

        userRepository.save(user);

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("OAuth2 로그인 성공: userId={}, email={}, status={}", user.getId(), user.getEmail(), user.getStatus());

        String redirectUrl;
        if (user.getStatus() == UserStatus.ACTIVE) {
            redirectUrl = String.format("%s/oauth/callback?accessToken=%s&refreshToken=%s",
                    frontendUrl, accessToken, refreshToken);
        } else {
            redirectUrl = switch (user.getStatus()) {
                case PENDING_APPROVAL -> String.format("%s/pending-approval?accessToken=%s", frontendUrl, accessToken);
                case EMAIL_VERIFICATION -> String.format("%s/verify-email?accessToken=%s", frontendUrl, accessToken);
                case REJECTED -> String.format("%s/rejected", frontendUrl);
                default -> String.format("%s/oauth/callback?accessToken=%s&refreshToken=%s",
                        frontendUrl, accessToken, refreshToken);
            };
        }
        response.sendRedirect(redirectUrl);
    }

    private User createUser(String googleId, String email, String name, String picture) {
        boolean isAdmin = adminEmail.equalsIgnoreCase(email);
        log.info("신규 사용자 생성: email={}, isAdmin={}", email, isAdmin);

        return userRepository.save(User.builder()
                .googleId(googleId)
                .email(email)
                .name(name)
                .profileImageUrl(picture)
                .role(isAdmin ? Role.ADMIN : Role.USER)
                .status(isAdmin ? UserStatus.ACTIVE : UserStatus.PENDING_APPROVAL)
                .settings(UserSettings.defaults())
                .build());
    }
}
