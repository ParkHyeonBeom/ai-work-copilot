package com.workcopilot.chat.controller;

import com.workcopilot.chat.service.ChatRoomService;
import com.workcopilot.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class UnreadController {

    private final ChatRoomService chatRoomService;

    @GetMapping("/unread")
    public ApiResponse<Integer> getUnreadCount(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        int unreadCount = chatRoomService.getUnreadCount(userId);
        return ApiResponse.ok(unreadCount);
    }
}
