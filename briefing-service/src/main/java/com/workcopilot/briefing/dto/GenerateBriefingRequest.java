package com.workcopilot.briefing.dto;

import java.time.LocalDate;

public record GenerateBriefingRequest(
        LocalDate date
) {
}
