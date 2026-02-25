package com.workcopilot.user.controller;

import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.user.entity.User;
import com.workcopilot.user.entity.UserStatus;
import com.workcopilot.user.repository.UserRepository;
import com.workcopilot.user.security.JwtProvider;
import com.workcopilot.user.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final VerificationService verificationService;
    private final JwtProvider jwtProvider;

    @PostMapping("/verify")
    public ApiResponse<Map<String, String>> verifyEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        verificationService.verifyCode(email, code);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.EMAIL_VERIFICATION) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이메일 인증 대상이 아닙니다.");
        }

        user.verify();
        userRepository.save(user);

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("이메일 인증 완료, 계정 활성화: email={}", email);
        return ApiResponse.ok(Map.of(
                "status", "ACTIVE",
                "accessToken", accessToken,
                "refreshToken", refreshToken
        ));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, String>> getStatus(Authentication authentication) {
        if (authentication == null) {
            return ApiResponse.ok(Map.of("status", "UNAUTHENTICATED"));
        }

        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return ApiResponse.ok(Map.of("status", user.getStatus().name()));
    }
}
