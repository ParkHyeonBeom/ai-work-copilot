package com.workcopilot.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workcopilot.ai.dto.AiResponse;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
public class LlmRouter {

    private final ChatModel openAiChatModel;
    private final ChatModel anthropicChatModel;
    private final ChatModel ollamaChatModel;
    private final RestClient ollamaRestClient;
    private final ObjectMapper objectMapper;
    private final boolean anthropicEnabled;
    private final boolean ollamaEnabled;

    @Value("${spring.ai.ollama.chat.options.model:llama3.1:8b-instruct-q4_K_M}")
    private String ollamaModel;

    @Value("${spring.ai.ollama.chat.options.num-predict:2048}")
    private int numPredict;

    public LlmRouter(
            @Qualifier("openAiChatModel") ChatModel openAiChatModel,
            @Qualifier("anthropicChatModel") ChatModel anthropicChatModel,
            @Qualifier("ollamaChatModel") ChatModel ollamaChatModel,
            RestClient ollamaRestClient,
            ObjectMapper objectMapper,
            @Value("${ai.anthropic.enabled:false}") boolean anthropicEnabled,
            @Value("${ai.ollama.enabled:false}") boolean ollamaEnabled
    ) {
        this.openAiChatModel = openAiChatModel;
        this.anthropicChatModel = anthropicChatModel;
        this.ollamaChatModel = ollamaChatModel;
        this.ollamaRestClient = ollamaRestClient;
        this.objectMapper = objectMapper;
        this.anthropicEnabled = anthropicEnabled;
        this.ollamaEnabled = ollamaEnabled;
    }

    /**
     * taskType에 따라 적절한 LLM으로 라우팅하여 요청을 처리한다.
     *
     * - "classify", "keyword" -> Ollama (Llama 3.1 8B) - 비용 절감
     * - "briefing" -> Claude Sonnet (긴 컨텍스트, 정확도)
     * - "summarize" -> Claude Sonnet (긴 컨텍스트)
     */
    public AiResponse route(String taskType, String promptText) {
        return switch (taskType.toLowerCase()) {
            case "classify", "keyword" -> callOllama(promptText, taskType);
            case "briefing", "summarize" -> callClaude(promptText, taskType);
            default -> callClaude(promptText, taskType);
        };
    }

    /**
     * Ollama 모델 호출 (Llama 3.1 8B, RestClient 사용)
     * Ollama가 비활성화되어 있거나 연결할 수 없으면 Claude로 폴백
     */
    private AiResponse callOllama(String promptText, String taskType) {
        if (!ollamaEnabled) {
            log.info("Ollama 비활성화 상태 - mock 응답 반환 (taskType={})", taskType);
            return createMockResponse(taskType, "ollama-llama3.1-8b (mock)");
        }

        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", ollamaModel,
                    "prompt", promptText,
                    "stream", false,
                    "options", Map.of(
                            "num_predict", numPredict,
                            "temperature", 0.3
                    )
            );

            String responseJson = ollamaRestClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode responseNode = objectMapper.readTree(responseJson);
            String result = responseNode.path("response").asText();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Ollama 호출 완료 (RestClient): taskType={}, processingTime={}ms", taskType, elapsed);
            return new AiResponse(result, "ollama-llama3.1-8b", elapsed);
        } catch (Exception e) {
            log.warn("Ollama 호출 실패, Claude로 폴백: taskType={}, error={}", taskType, e.getMessage());
            return callClaude(promptText, taskType);
        }
    }

    /**
     * Ollama 모델 직접 호출 (RestClient 사용, 타임아웃 180초)
     */
    private AiResponse callOllamaDirectly(String promptText, String taskType) {
        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", ollamaModel,
                    "prompt", promptText,
                    "stream", false,
                    "options", Map.of(
                            "num_predict", numPredict,
                            "temperature", 0.3
                    )
            );

            String responseJson = ollamaRestClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode responseNode = objectMapper.readTree(responseJson);
            String result = responseNode.path("response").asText();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Ollama 호출 완료 (RestClient): taskType={}, processingTime={}ms", taskType, elapsed);
            return new AiResponse(result, "ollama-llama3.1-8b", elapsed);
        } catch (Exception e) {
            log.error("Ollama 호출 실패: taskType={}, error={}", taskType, e.getMessage(), e);
            log.info("Ollama 연결 실패 - mock 응답 반환 (taskType={})", taskType);
            return createMockResponse(taskType, "ollama-llama3.1-8b (mock)");
        }
    }

    /**
     * Claude Sonnet 모델 호출
     */
    private AiResponse callClaude(String promptText, String taskType) {
        if (!anthropicEnabled) {
            // Ollama가 활성화되어 있으면 Ollama로 폴백, 아니면 OpenAI
            if (ollamaEnabled) {
                log.info("Claude 비활성화 상태 - Ollama로 폴백 (taskType={})", taskType);
                return callOllamaDirectly(promptText, taskType);
            }
            log.info("Claude 비활성화 상태 - OpenAI로 폴백 시도 (taskType={})", taskType);
            return callOpenAi(promptText, taskType);
        }

        long startTime = System.currentTimeMillis();
        try {
            ChatClient chatClient = ChatClient.create(anthropicChatModel);
            String result = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Claude 호출 완료: taskType={}, processingTime={}ms", taskType, elapsed);
            return new AiResponse(result, "claude-sonnet-4", elapsed);
        } catch (Exception e) {
            log.error("Claude 호출 실패: taskType={}, error={}", taskType, e.getMessage(), e);

            // API 키 미설정 시 mock 응답 반환
            if (e.getMessage() != null && (e.getMessage().contains("401") || e.getMessage().contains("Invalid") || e.getMessage().contains("api_key"))) {
                log.info("Claude API 키 미설정 - mock 응답 반환 (taskType={})", taskType);
                return createMockResponse(taskType, "claude-sonnet-4 (mock)");
            }
            throw new BusinessException(ErrorCode.LLM_ERROR, "LLM 호출 실패: " + e.getMessage());
        }
    }

    /**
     * OpenAI GPT-4o 모델 호출 (폴백용)
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
            case "classify", "keyword" -> ollamaEnabled ? "ollama-llama3.1-8b" : (anthropicEnabled ? "claude-sonnet-4" : "gpt-4o");
            case "briefing", "summarize" -> anthropicEnabled ? "claude-sonnet-4" : "gpt-4o";
            default -> anthropicEnabled ? "claude-sonnet-4" : "gpt-4o";
        };
    }
}
