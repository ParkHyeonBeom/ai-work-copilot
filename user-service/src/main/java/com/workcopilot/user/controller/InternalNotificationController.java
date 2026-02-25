package com.workcopilot.user.controller;

import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.user.dto.MeetingNotificationRequest;
import com.workcopilot.user.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/meeting")
    public ApiResponse<Void> notifyTeamMeeting(@Valid @RequestBody MeetingNotificationRequest request) {
        notificationService.notifyTeamMeeting(request);
        return ApiResponse.ok(null);
    }
}
