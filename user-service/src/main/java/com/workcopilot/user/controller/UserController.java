package com.workcopilot.user.controller;

import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.user.dto.OnboardingRequest;
import com.workcopilot.user.dto.UpdateSettingsRequest;
import com.workcopilot.user.dto.UserResponse;
import com.workcopilot.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(userService.getMe(userId));
    }

    @PutMapping("/me/settings")
    public ApiResponse<UserResponse> updateSettings(Authentication authentication,
                                                     @Valid @RequestBody UpdateSettingsRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(userService.updateSettings(userId, request));
    }

    @PostMapping("/me/onboarding")
    public ApiResponse<UserResponse> completeOnboarding(Authentication authentication,
                                                         @Valid @RequestBody OnboardingRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(userService.completeOnboarding(userId, request));
    }
}
