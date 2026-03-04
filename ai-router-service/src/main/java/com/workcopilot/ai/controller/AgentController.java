package com.workcopilot.ai.controller;

import com.workcopilot.ai.agent.AgentService;
import com.workcopilot.ai.dto.*;
import com.workcopilot.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ai/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * AI 에이전트와 채팅한다.
     * conversationId가 null이면 새 대화를 생성하고, 있으면 기존 대화를 이어간다.
     */
    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(
            Authentication authentication,
            @Valid @RequestBody AgentChatRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) authentication.getPrincipal();
        String token = extractToken(httpRequest);

        log.info("에이전트 채팅 API 요청: userId={}, conversationId={}", userId, request.conversationId());
        AgentChatResponse response = agentService.chat(userId, token, request);
        return ApiResponse.ok(response);
    }

    /**
     * 사용자의 대화 목록을 조회한다.
     */
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationDto>> getConversations(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("대화 목록 조회 API: userId={}", userId);
        List<ConversationDto> conversations = agentService.getConversations(userId);
        return ApiResponse.ok(conversations);
    }

    /**
     * 특정 대화의 상세 정보(메시지 포함)를 조회한다.
     */
    @GetMapping("/conversations/{id}")
    public ApiResponse<ConversationDetailDto> getConversation(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("대화 상세 조회 API: userId={}, conversationId={}", userId, id);
        ConversationDetailDto conversation = agentService.getConversation(userId, id);
        return ApiResponse.ok(conversation);
    }

    /**
     * 대화를 삭제(소프트 삭제)한다.
     */
    @DeleteMapping("/conversations/{id}")
    public ApiResponse<Void> deleteConversation(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("대화 삭제 API: userId={}, conversationId={}", userId, id);
        agentService.deleteConversation(userId, id);
        return ApiResponse.ok(null, "대화가 삭제되었습니다.");
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
