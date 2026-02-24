package com.workcopilot.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateSettingsRequest(
        List<String> monitoredCalendarIds,
        List<String> monitoredDriveFolderIds,
        List<String> importantDomains,
        List<String> excludeLabels,
        @NotBlank String workStartTime,
        @NotBlank String workEndTime,
        @NotBlank String language,
        @NotBlank String timezone
) {
}
