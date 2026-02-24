package com.workcopilot.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT("C001", "잘못된 입력입니다.", 400),
    INTERNAL_ERROR("C002", "서버 내부 오류가 발생했습니다.", 500),
    NOT_FOUND("C003", "리소스를 찾을 수 없습니다.", 404),

    // Auth
    UNAUTHORIZED("A001", "인증이 필요합니다.", 401),
    FORBIDDEN("A002", "권한이 없습니다.", 403),
    INVALID_TOKEN("A003", "유효하지 않은 토큰입니다.", 401),
    EXPIRED_TOKEN("A004", "만료된 토큰입니다.", 401),
    OAUTH_FAILED("A005", "OAuth 인증에 실패했습니다.", 401),

    // User
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다.", 404),
    DUPLICATE_EMAIL("U002", "이미 등록된 이메일입니다.", 409),

    // Integration
    GOOGLE_API_ERROR("I001", "Google API 호출에 실패했습니다.", 502),
    TOKEN_REFRESH_FAILED("I002", "토큰 갱신에 실패했습니다.", 401),

    // AI
    LLM_ERROR("AI001", "LLM 호출에 실패했습니다.", 502),
    EMBEDDING_ERROR("AI002", "임베딩 생성에 실패했습니다.", 502),

    // Briefing
    BRIEFING_NOT_FOUND("B001", "브리핑을 찾을 수 없습니다.", 404);

    private final String code;
    private final String message;
    private final int httpStatus;
}
