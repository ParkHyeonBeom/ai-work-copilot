package com.workcopilot.user.controller;

import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.user.dto.GoogleTokenResponse;
import com.workcopilot.user.dto.UserInfoDto;
import com.workcopilot.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @GetMapping("/{userId}/google-token")
    public ApiResponse<GoogleTokenResponse> getGoogleToken(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getGoogleToken(userId));
    }

    @GetMapping("/{userId}/info")
    public ApiResponse<UserInfoDto> getUserInfo(@PathVariable Long userId) {
        return ApiResponse.ok(
                userRepository.findById(userId)
                        .map(UserInfoDto::from)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
        );
    }
}
