package com.workcopilot.briefing.dto;

import com.workcopilot.briefing.entity.Briefing;
import com.workcopilot.briefing.entity.BriefingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BriefingListResponse(
        Long id,
        LocalDate briefingDate,
        BriefingStatus status,
        String summary,
        LocalDateTime createdAt
) {
    public static BriefingListResponse from(Briefing briefing) {
        return new BriefingListResponse(
                briefing.getId(),
                briefing.getBriefingDate(),
                briefing.getStatus(),
                briefing.getSummary(),
                briefing.getCreatedAt()
        );
    }
}
