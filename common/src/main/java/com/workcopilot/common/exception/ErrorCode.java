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
    PENDING_APPROVAL("A006", "관리자 승인 대기 중입니다.", 403),
    EMAIL_VERIFICATION_REQUIRED("A007", "이메일 인증이 필요합니다.", 403),
    INVALID_VERIFICATION_CODE("A008", "인증코드가 유효하지 않습니다.", 400),
    VERIFICATION_CODE_EXPIRED("A009", "인증코드가 만료되었습니다.", 400),
    USER_REJECTED("A010", "가입이 거부되었습니다.", 403),
    ADMIN_ONLY("A011", "관리자만 접근 가능합니다.", 403),

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
    BRIEFING_NOT_FOUND("B001", "브리핑을 찾을 수 없습니다.", 404),

    // Chat
    CHAT_ROOM_NOT_FOUND("CH001", "채팅방을 찾을 수 없습니다.", 404),
    CHAT_NOT_PARTICIPANT("CH002", "채팅방 참여자가 아닙니다.", 403),
    CHAT_FILE_TOO_LARGE("CH003", "파일 크기가 10MB를 초과합니다.", 400),
    CHAT_FILE_TYPE_NOT_ALLOWED("CH004", "허용되지 않는 파일 형식입니다.", 400),
    CHAT_MESSAGE_NOT_FOUND("CH005", "메시지를 찾을 수 없습니다.", 404),
    CHAT_NOT_MESSAGE_OWNER("CH006", "본인의 메시지만 삭제할 수 있습니다.", 403),

    // AI Agent
    AGENT_CONVERSATION_NOT_FOUND("AG001", "대화를 찾을 수 없습니다.", 404),
    AGENT_CONTEXT_COLLECTION_FAILED("AG002", "컨텍스트 수집에 실패했습니다.", 502);

    private final String code;
    private final String message;
    private final int httpStatus;
}
