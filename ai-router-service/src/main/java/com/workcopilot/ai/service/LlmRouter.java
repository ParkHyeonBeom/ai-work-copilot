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

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LlmRouter {

    private final ChatModel openAiChatModel;
    private final ChatModel anthropicChatModel;
    private final ChatModel ollamaChatModel;
    private final RestClient ollamaRestClient;
    private final RestClient geminiRestClient;
    private final ObjectMapper objectMapper;
    private final boolean anthropicEnabled;
    private final boolean ollamaEnabled;
    private final boolean geminiEnabled;
    private final String geminiApiKey;
    private final String geminiModel;

    @Value("${spring.ai.ollama.chat.options.model:llama3.1:8b-instruct-q4_K_M}")
    private String ollamaModel;

    @Value("${spring.ai.ollama.chat.options.num-predict:2048}")
    private int numPredict;

    public LlmRouter(
            @Qualifier("openAiChatModel") ChatModel openAiChatModel,
            @Qualifier("anthropicChatModel") ChatModel anthropicChatModel,
            @Qualifier("ollamaChatModel") ChatModel ollamaChatModel,
            RestClient ollamaRestClient,
            RestClient geminiRestClient,
            ObjectMapper objectMapper,
            @Value("${ai.anthropic.enabled:false}") boolean anthropicEnabled,
            @Value("${ai.ollama.enabled:false}") boolean ollamaEnabled,
            @Value("${ai.gemini.enabled:false}") boolean geminiEnabled,
            @Value("${ai.gemini.api-key:}") String geminiApiKey,
            @Value("${ai.gemini.model:gemini-2.5-flash}") String geminiModel
    ) {
        this.openAiChatModel = openAiChatModel;
        this.anthropicChatModel = anthropicChatModel;
        this.ollamaChatModel = ollamaChatModel;
        this.ollamaRestClient = ollamaRestClient;
        this.geminiRestClient = geminiRestClient;
        this.objectMapper = objectMapper;
        this.anthropicEnabled = anthropicEnabled;
        this.ollamaEnabled = ollamaEnabled;
        this.geminiEnabled = geminiEnabled;
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
    }

    /**
     * taskType에 따라 적절한 LLM으로 라우팅하여 요청을 처리한다.
     *
     * - "classify", "keyword" → Ollama (Llama 3.1 8B) - 비용 절감
     * - "briefing", "summarize" → Gemini → Ollama → Mock (기존: Claude → Ollama)
     *
     * 실제 폴백 체인: briefing → Gemini → Ollama → mock
     */
    public AiResponse route(String taskType, String promptText) {
        return switch (taskType.toLowerCase()) {
            case "classify", "keyword" -> callOllama(promptText, taskType);
            case "briefing", "summarize" -> callGemini(promptText, taskType);
            default -> callGemini(promptText, taskType);
        };
    }

    /**
     * Gemini (Google AI Studio) 모델 호출
     * Gemini가 비활성화되어 있거나 실패하면 Ollama로 폴백
     */
    private AiResponse callGemini(String promptText, String taskType) {
        if (!geminiEnabled || geminiApiKey == null || geminiApiKey.isBlank()) {
            log.info("Gemini 비활성화 상태 - Ollama로 폴백 (taskType={})", taskType);
            return callOllamaForBriefing(promptText, taskType);
        }

        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", promptText)
                            ))
                    )
            );

            String responseJson = geminiRestClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", geminiModel, geminiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode responseNode = objectMapper.readTree(responseJson);
            String result = responseNode
                    .path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Gemini 호출 완료: taskType={}, model={}, processingTime={}ms", taskType, geminiModel, elapsed);
            return new AiResponse(result, geminiModel, elapsed);
        } catch (Exception e) {
            log.warn("Gemini 호출 실패, Ollama로 폴백: taskType={}, error={}", taskType, e.getMessage());
            return callOllamaForBriefing(promptText, taskType);
        }
    }

    /**
     * briefing/summarize용 Ollama 폴백 (Gemini 실패 시)
     * Ollama도 실패하면 Claude → mock 순으로 폴백
     */
    private AiResponse callOllamaForBriefing(String promptText, String taskType) {
        if (!ollamaEnabled) {
            log.info("Ollama 비활성화 상태 - Claude로 폴백 (taskType={})", taskType);
            return callClaude(promptText, taskType);
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
            log.info("Ollama 호출 완료 (briefing 폴백): taskType={}, processingTime={}ms", taskType, elapsed);
            return new AiResponse(result, "ollama-llama3.1-8b", elapsed);
        } catch (Exception e) {
            log.warn("Ollama 호출 실패, mock 응답 반환: taskType={}, error={}", taskType, e.getMessage());
            return createMockResponse(taskType, "ollama-llama3.1-8b (mock)");
        }
    }

    /**
     * Ollama 모델 호출 (Llama 3.1 8B, RestClient 사용)
     * Ollama가 비활성화되어 있거나 연결할 수 없으면 mock 응답 반환
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
            log.warn("Ollama 호출 실패: taskType={}, error={}", taskType, e.getMessage());
            return createMockResponse(taskType, "ollama-llama3.1-8b (mock)");
        }
    }

    /**
     * Claude Sonnet 모델 호출
     */
    private AiResponse callClaude(String promptText, String taskType) {
        if (!anthropicEnabled) {
            log.info("Claude 비활성화 상태 - mock 응답 반환 (taskType={})", taskType);
            return createMockResponse(taskType, "claude-sonnet-4 (mock)");
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

            if (e.getMessage() != null && (e.getMessage().contains("401") || e.getMessage().contains("Invalid") || e.getMessage().contains("api_key"))) {
                log.info("Claude API 키 미설정 - mock 응답 반환 (taskType={})", taskType);
                return createMockResponse(taskType, "claude-sonnet-4 (mock)");
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
            case "classify", "keyword" -> ollamaEnabled ? "ollama-llama3.1-8b" : "ollama-llama3.1-8b (mock)";
            case "briefing", "summarize" -> geminiEnabled ? geminiModel : (ollamaEnabled ? "ollama-llama3.1-8b" : "mock");
            default -> geminiEnabled ? geminiModel : "mock";
        };
    }
}
