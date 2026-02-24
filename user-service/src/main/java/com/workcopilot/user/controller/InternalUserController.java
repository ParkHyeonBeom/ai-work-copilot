package com.workcopilot.user.controller;

import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.user.dto.GoogleTokenResponse;
import com.workcopilot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}/google-token")
    public ApiResponse<GoogleTokenResponse> getGoogleToken(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getGoogleToken(userId));
    }
}
