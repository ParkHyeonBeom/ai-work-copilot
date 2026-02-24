package com.workcopilot.briefing.dto;

import com.workcopilot.briefing.entity.Briefing;
import com.workcopilot.briefing.entity.BriefingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BriefingResponse(
        Long id,
        Long userId,
        LocalDate briefingDate,
        BriefingStatus status,
        String summary,
        String fullContent,
        List<String> keyPoints,
        List<String> actionItems,
        int eventCount,
        int emailCount,
        int fileCount,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static BriefingResponse from(Briefing briefing) {
        return new BriefingResponse(
                briefing.getId(),
                briefing.getUserId(),
                briefing.getBriefingDate(),
                briefing.getStatus(),
                briefing.getSummary(),
                briefing.getFullContent(),
                briefing.getKeyPoints(),
                briefing.getActionItems(),
                briefing.getEventCount(),
                briefing.getEmailCount(),
                briefing.getFileCount(),
                briefing.getCreatedAt(),
                briefing.getCompletedAt()
        );
    }
}
