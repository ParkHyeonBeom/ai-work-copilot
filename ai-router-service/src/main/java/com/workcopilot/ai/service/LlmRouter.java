package com.workcopilot.ai.service;

import com.workcopilot.ai.dto.AiResponse;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LlmRouter {

    private final ChatModel openAiChatModel;
    private final ChatModel ollamaChatModel;
    private final boolean ollamaEnabled;

    public LlmRouter(
            @Qualifier("openAiChatModel") ChatModel openAiChatModel,
            @Qualifier("ollamaChatModel") ChatModel ollamaChatModel,
            @Value("${ai.ollama.enabled:false}") boolean ollamaEnabled
    ) {
        this.openAiChatModel = openAiChatModel;
        this.ollamaChatModel = ollamaChatModel;
        this.ollamaEnabled = ollamaEnabled;
    }

    /**
     * taskType에 따라 적절한 LLM으로 라우팅하여 요청을 처리한다.
     *
     * - "classify", "keyword" -> Ollama (Llama 3.1 8B) - 비용 절감
     * - "briefing" -> OpenAI GPT-4o (Function Calling) - 정확도
     * - "summarize" -> OpenAI GPT-4o (fallback) - 긴 컨텍스트
     */
    public AiResponse route(String taskType, String promptText) {
        return switch (taskType.toLowerCase()) {
            case "classify", "keyword" -> callOllama(promptText, taskType);
            case "briefing" -> callOpenAi(promptText, taskType);
            case "summarize" -> callOpenAi(promptText, taskType);
            default -> callOpenAi(promptText, taskType);
        };
    }

    /**
     * Ollama 모델 호출 (Llama 3.1 8B)
     * Ollama가 비활성화되어 있거나 연결할 수 없으면 mock 응답 반환
     */
    private AiResponse callOllama(String promptText, String taskType) {
        if (!ollamaEnabled) {
            log.info("Ollama 비활성화 상태 - mock 응답 반환 (taskType={})", taskType);
            return createMockResponse(taskType, "ollama-llama3.1-8b (mock)");
        }

        long startTime = System.currentTimeMillis();
        try {
            ChatClient chatClient = ChatClient.create(ollamaChatModel);
            String result = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Ollama 호출 완료: taskType={}, processingTime={}ms", taskType, elapsed);
            return new AiResponse(result, "ollama-llama3.1-8b", elapsed);
        } catch (Exception e) {
            log.warn("Ollama 호출 실패, OpenAI로 폴백: taskType={}, error={}", taskType, e.getMessage());
            return callOpenAi(promptText, taskType);
        }
    }

    /**
     * OpenAI GPT-4o 모델 호출
     */
    private AiResponse callOpenAi(String promptText, String taskType) {
        long startTime = System.currentTimeMillis();
        try {
            ChatClient chatClient = ChatClient.create(openAiChatModel);
            String result = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("OpenAI 호출 완료: taskType={}, processingTime={}ms", taskType, elapsed);
            return new AiResponse(result, "gpt-4o", elapsed);
        } catch (Exception e) {
            log.error("OpenAI 호출 실패: taskType={}, error={}", taskType, e.getMessage(), e);

            // local 환경에서 API 키가 dummy인 경우 mock 응답 반환
            if (e.getMessage() != null && (e.getMessage().contains("401") || e.getMessage().contains("Invalid"))) {
                log.info("OpenAI API 키 미설정 - mock 응답 반환 (taskType={})", taskType);
                return createMockResponse(taskType, "gpt-4o (mock)");
            }
            throw new BusinessException(ErrorCode.LLM_ERROR, "LLM 호출 실패: " + e.getMessage());
        }
    }

    /**
     * LLM 서비스를 사용할 수 없을 때 로컬 테스트용 mock 응답 생성
     */
    private AiResponse createMockResponse(String taskType, String model) {
        String mockResult = switch (taskType.toLowerCase()) {
            case "classify" -> """
                    {"importance": "medium", "category": "general", "confidence": 0.85}""";
            case "keyword" -> """
                    {"keywords": ["업무", "회의", "프로젝트"]}""";
            case "briefing" -> """
                    {
                      "summary": "[Mock] 오늘의 업무 브리핑입니다. 실제 LLM이 연결되면 상세 분석이 제공됩니다.",
                      "keyPoints": ["LLM 미연결 상태의 테스트 응답입니다"],
                      "actionItems": ["LLM 서비스 연결을 확인하세요"]
                    }""";
            case "summarize" -> "[Mock] 문서 요약 결과입니다. 실제 LLM이 연결되면 상세 요약이 제공됩니다.";
            default -> "[Mock] 처리 결과입니다.";
        };

        return new AiResponse(mockResult, model, 0);
    }

    /**
     * 주어진 taskType에 대해 사용될 모델명을 반환한다.
     */
    public String getModelForTask(String taskType) {
        return switch (taskType.toLowerCase()) {
            case "classify", "keyword" -> ollamaEnabled ? "ollama-llama3.1-8b" : "gpt-4o";
            case "briefing", "summarize" -> "gpt-4o";
            default -> "gpt-4o";
        };
    }
}
