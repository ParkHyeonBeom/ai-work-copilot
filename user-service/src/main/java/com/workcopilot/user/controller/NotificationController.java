package com.workcopilot.user.controller;

import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.user.dto.NotificationResponse;
import com.workcopilot.user.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getNotifications(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<NotificationResponse> notifications = notificationService.getRecentNotifications(userId);
        long unreadCount = notificationService.getUnreadCount(userId);
        return ApiResponse.ok(Map.of(
                "notifications", notifications,
                "unreadCount", unreadCount
        ));
    }

    @PostMapping("/read")
    public ApiResponse<Void> markAllRead(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAllRead(userId);
        return ApiResponse.ok(null);
    }
}
