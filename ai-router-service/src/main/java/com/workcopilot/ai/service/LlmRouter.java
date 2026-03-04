package com.workcopilot.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workcopilot.ai.dto.AiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LlmRouter {

    private final RestClient geminiRestClient;
    private final ObjectMapper objectMapper;
    private final String geminiApiKey;
    private final String geminiModel;

    public LlmRouter(
            RestClient geminiRestClient,
            ObjectMapper objectMapper,
            @Value("${ai.gemini.api-key:}") String geminiApiKey,
            @Value("${ai.gemini.model:gemini-2.5-flash}") String geminiModel
    ) {
        this.geminiRestClient = geminiRestClient;
        this.objectMapper = objectMapper;
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
    }

    /**
     * taskType에 따라 Gemini API로 라우팅하여 요청을 처리한다.
     * API 키가 없으면 mock 응답 반환.
     */
    public AiResponse route(String taskType, String promptText) {
        return callGemini(promptText, taskType);
    }

    /**
     * Gemini (Google AI Studio) 모델 호출
     * API 키가 없거나 호출 실패 시 mock 응답 반환
     */
    private AiResponse callGemini(String promptText, String taskType) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.info("Gemini API 키 미설정 - mock 응답 반환 (taskType={})", taskType);
            return createMockResponse(taskType, geminiModel + " (mock)");
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
            log.warn("Gemini 호출 실패, mock 응답 반환: taskType={}, error={}", taskType, e.getMessage());
            return createMockResponse(taskType, geminiModel + " (mock)");
        }
    }

    /**
     * LLM 서비스를 사용할 수 없을 때 로컬 테스트용 mock 응답 생성
     */
    AiResponse createMockResponse(String taskType, String model) {
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
            case "agent" -> "[Mock] AI 어시스턴트 응답입니다. Gemini API 키를 설정하면 실제 응답이 제공됩니다.";
            default -> "[Mock] 처리 결과입니다.";
        };

        return new AiResponse(mockResult, model, 0);
    }

    /**
     * 주어진 taskType에 대해 사용될 모델명을 반환한다.
     */
    public String getModelForTask(String taskType) {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            return geminiModel;
        }
        return geminiModel + " (mock)";
    }
}
