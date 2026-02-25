package com.workcopilot.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MeetingNotificationRequest(
        @NotNull Long creatorUserId,
        @NotBlank String meetingTitle,
        @NotBlank String meetingTime,
        String location,
        List<String> attendeeEmails
) {
}
