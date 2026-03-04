package com.workcopilot.chat.dto;

import java.util.List;

public record ReactionDto(String emoji, int count, List<ReactionUserDto> users, boolean myReaction) {
}
