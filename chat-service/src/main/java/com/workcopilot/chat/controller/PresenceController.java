package com.workcopilot.chat.controller;

import com.workcopilot.chat.service.PresenceService;
import com.workcopilot.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/chat/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/online")
    public ApiResponse<Set<Long>> getOnlineUsers() {
        return ApiResponse.ok(presenceService.getOnlineUserIds());
    }
}
