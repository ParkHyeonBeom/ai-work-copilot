package com.workcopilot.ai.agent;

import com.workcopilot.ai.dto.*;
import com.workcopilot.ai.entity.ConversationHistory;
import com.workcopilot.ai.entity.ConversationMessage;
import com.workcopilot.ai.repository.ConversationHistoryRepository;
import com.workcopilot.ai.repository.ConversationMessageRepository;
import com.workcopilot.ai.service.LlmRouter;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AgentService {

    private final AgentContextCollector agentContextCollector;
    private final AgentContextPreprocessor agentContextPreprocessor;
    private final AgentPromptBuilder agentPromptBuilder;
    private final LlmRouter llmRouter;
    private final ConversationHistoryRepository conversationHistoryRepository;
    private final ConversationMessageRepository conversationMessageRepository;

    /**
     * 에이전트 채팅 처리:
     * 1. 대화 가져오기 또는 새로 생성
     * 2. 컨텍스트 수집
     * 3. 컨텍스트 전처리
     * 4. 최근 대화 히스토리 조회
     * 5. 프롬프트 생성
     * 6. LLM 호출
     * 7. 메시지 저장
     * 8. 대화 제목 업데이트
     */
    public AgentChatResponse chat(Long userId, String token, AgentChatRequest request) {
        log.info("에이전트 채팅 요청: userId={}, conversationId={}", userId, request.conversationId());

        // 1. 대화 가져오기 또는 새로 생성
        ConversationHistory conversation = getOrCreateConversation(userId, request.conversationId());

        // 2. 컨텍스트 수집
        AgentContextCollector.AgentContext context;
        try {
            context = agentContextCollector.collectContext(userId, token);
        } catch (Exception e) {
            log.error("컨텍스트 수집 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.AGENT_CONTEXT_COLLECTION_FAILED);
        }

        // 3. 컨텍스트 전처리
        AgentContextPreprocessor.PreprocessedContext preprocessed =
                agentContextPreprocessor.preprocess(context, request.message());

        // 4. 최근 대화 히스토리 조회 (최근 10개, 역순)
        List<ConversationMessage> recentHistory =
                conversationMessageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(conversation.getId());

        // 5. 프롬프트 생성
        String prompt = agentPromptBuilder.buildPrompt(
                request.message(), preprocessed.contextText(), recentHistory);

        // 6. LLM 호출
        AiResponse aiResponse = llmRouter.route("agent", prompt);

        // 7. 사용자 메시지 저장
        ConversationMessage userMessage = ConversationMessage.builder()
                .conversation(conversation)
                .role("user")
                .content(request.message())
                .build();
        conversationMessageRepository.save(userMessage);

        // 어시스턴트 메시지 저장
        String contextSourcesStr = String.join(",", preprocessed.usedSources());
        ConversationMessage assistantMessage = ConversationMessage.builder()
                .conversation(conversation)
                .role("assistant")
                .content(aiResponse.result())
                .contextSources(contextSourcesStr)
                .model(aiResponse.model())
                .processingTimeMs(aiResponse.processingTimeMs())
                .build();
        conversationMessageRepository.save(assistantMessage);

        // 8. 대화 제목 업데이트 (첫 메시지인 경우)
        if (conversation.getTitle() == null || conversation.getTitle().isBlank()) {
            String title = request.message().length() > 50
                    ? request.message().substring(0, 50) + "..."
                    : request.message();
            conversation.updateTitle(title);
        }

        log.info("에이전트 채팅 완료: userId={}, conversationId={}, model={}, processingTime={}ms",
                userId, conversation.getId(), aiResponse.model(), aiResponse.processingTimeMs());

        return new AgentChatResponse(
                conversation.getId(),
                aiResponse.result(),
                aiResponse.model(),
                aiResponse.processingTimeMs(),
                preprocessed.usedSources()
        );
    }

    /**
     * 사용자의 대화 목록을 조회한다.
     */
    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(Long userId) {
        log.info("대화 목록 조회: userId={}", userId);
        return conversationHistoryRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)
                .stream()
                .map(ConversationDto::from)
                .toList();
    }

    /**
     * 특정 대화의 상세 정보(메시지 포함)를 조회한다.
     */
    @Transactional(readOnly = true)
    public ConversationDetailDto getConversation(Long userId, Long conversationId) {
        log.info("대화 상세 조회: userId={}, conversationId={}", userId, conversationId);

        ConversationHistory conversation = conversationHistoryRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_CONVERSATION_NOT_FOUND));

        List<MessageDto> messages = conversationMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(MessageDto::from)
                .toList();

        return new ConversationDetailDto(
                conversation.getId(),
                conversation.getTitle(),
                messages
        );
    }

    /**
     * 대화를 소프트 삭제(비활성화)한다.
     */
    public void deleteConversation(Long userId, Long conversationId) {
        log.info("대화 삭제: userId={}, conversationId={}", userId, conversationId);

        ConversationHistory conversation = conversationHistoryRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_CONVERSATION_NOT_FOUND));

        conversation.deactivate();
        log.info("대화 비활성화 완료: conversationId={}", conversationId);
    }

    private ConversationHistory getOrCreateConversation(Long userId, Long conversationId) {
        if (conversationId != null) {
            return conversationHistoryRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_CONVERSATION_NOT_FOUND));
        }

        // 새 대화 생성
        ConversationHistory newConversation = ConversationHistory.builder()
                .userId(userId)
                .build();
        return conversationHistoryRepository.save(newConversation);
    }
}
