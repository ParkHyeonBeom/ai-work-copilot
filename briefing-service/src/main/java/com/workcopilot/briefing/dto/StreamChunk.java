package com.workcopilot.briefing.dto;

public record StreamChunk(
        String content,
        boolean done
) {
}
