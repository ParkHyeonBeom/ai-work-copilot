package com.workcopilot.chat.dto;

import com.workcopilot.chat.entity.ChatRoomType;

import java.util.List;

public record ChatRoomCreateRequest(
        ChatRoomType type,
        String name,
        List<Long> memberIds
) {
}
