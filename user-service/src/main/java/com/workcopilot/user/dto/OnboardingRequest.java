package com.workcopilot.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record OnboardingRequest(
        List<String> monitoredCalendarIds,
        List<String> monitoredDriveFolderIds,
        List<String> importantDomains,
        @NotBlank String workStartTime,
        @NotBlank String workEndTime,
        @NotBlank String timezone
) {
}
