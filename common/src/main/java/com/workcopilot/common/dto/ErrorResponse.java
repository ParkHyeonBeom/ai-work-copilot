package com.workcopilot.common.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class ErrorResponse {
    private final int status;
    private final String errorCode;
    private final String message;
    private final Map<String, String> fieldErrors;
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();
}
