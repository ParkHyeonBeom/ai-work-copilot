package com.workcopilot.chat.dto;

import java.util.List;

public record InviteMembersRequest(
        List<Long> memberIds
) {
}
