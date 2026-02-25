package com.workcopilot.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workcopilot.ai.dto.AiResponse;
import com.workcopilot.ai.dto.BriefingRequest;
import com.workcopilot.ai.dto.BriefingResponse;
import com.workcopilot.ai.service.prompt.BriefingPromptBuilder;
import com.workcopilot.ai.service.prompt.ClassificationPromptBuilder;
import com.workcopilot.ai.service.prompt.SummarizationPromptBuilder;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final LlmRouter llmRouter;
    private final BriefingPromptBuilder briefingPromptBuilder;
    private final ClassificationPromptBuilder classificationPromptBuilder;
    private final SummarizationPromptBuilder summarizationPromptBuilder;
    private final ObjectMapper objectMapper;

    /**
     * 일일 업무 브리핑을 생성한다.
     * BriefingRequest의 일정, 이메일, 드라이브 파일 정보를 종합하여
     * LLM을 통해 브리핑을 생성한다.
     */
    public BriefingResponse generateBriefing(BriefingRequest request) {
        log.info("브리핑 생성 시작: userId={}", request.userId());

        String prompt = briefingPromptBuilder.build(request);
        AiResponse aiResponse = llmRouter.route("briefing", prompt);

        BriefingResponse briefingResponse = parseBriefingResponse(aiResponse.result());

        log.info("브리핑 생성 완료: userId={}, model={}, processingTime={}ms",
                request.userId(), aiResponse.model(), aiResponse.processingTimeMs());

        return briefingResponse;
    }

    /**
     * 이메일 내용을 분류한다.
     * 중요도, 카테고리, 신뢰도를 반환한다.
     */
    public AiResponse classifyContent(String content) {
        log.info("콘텐츠 분류 시작: contentLength={}", content.length());

        String prompt = classificationPromptBuilder.build(content);
        AiResponse response = llmRouter.route("classify", prompt);

        log.info("콘텐츠 분류 완료: model={}, processingTime={}ms",
                response.model(), response.processingTimeMs());

        return response;
    }

    /**
     * 문서를 요약한다.
     */
    public AiResponse summarizeDocument(String content) {
        log.info("문서 요약 시작: contentLength={}", content.length());

        String prompt = summarizationPromptBuilder.build(content);
        AiResponse response = llmRouter.route("summarize", prompt);

        log.info("문서 요약 완료: model={}, processingTime={}ms",
                response.model(), response.processingTimeMs());

        return response;
    }

    /**
     * LLM 응답을 BriefingResponse로 파싱한다.
     * JSON 파싱에 실패하면 JSON 추출을 시도하고, 그래도 실패하면 원문을 fullContent로 사용한다.
     */
    private BriefingResponse parseBriefingResponse(String llmResult) {
        // 먼저 JSON 추출 시도
        String jsonContent = extractJson(llmResult);

        try {
            Map<String, Object> resultMap = objectMapper.readValue(jsonContent, new TypeReference<>() {});

            String summary = (String) resultMap.getOrDefault("summary", "브리핑 요약을 생성할 수 없습니다.");
            String fullContent = (String) resultMap.getOrDefault("fullContent", llmResult);

            @SuppressWarnings("unchecked")
            List<String> keyPoints = resultMap.containsKey("keyPoints")
                    ? (List<String>) resultMap.get("keyPoints")
                    : List.of();

            @SuppressWarnings("unchecked")
            List<String> actionItems = resultMap.containsKey("actionItems")
                    ? (List<String>) resultMap.get("actionItems")
                    : List.of();

            return new BriefingResponse(summary, fullContent, keyPoints, actionItems);
        } catch (JsonProcessingException e) {
            log.warn("LLM 응답 JSON 파싱 실패, 원문을 fullContent로 사용: {}", e.getMessage());

            // 원문이 비어있거나 너무 짧으면 기본 메시지 사용
            String content = (llmResult != null && llmResult.trim().length() > 10)
                    ? llmResult.trim()
                    : "브리핑 생성 중 응답을 받지 못했습니다.";

            return new BriefingResponse(
                    "브리핑이 생성되었습니다.",
                    content,
                    List.of(),
                    List.of()
            );
        }
    }

    /**
     * LLM 응답에서 JSON 부분을 추출한다.
     * ```json ... ``` 블록이나 { } 블록을 찾아서 반환한다.
     */
    private String extractJson(String text) {
        if (text == null || text.isBlank()) {
            return "{}";
        }

        String trimmed = text.trim();

        // 이미 순수 JSON이면 그대로 반환
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        // ```json ... ``` 블록 추출
        int jsonBlockStart = trimmed.indexOf("```json");
        if (jsonBlockStart >= 0) {
            int jsonStart = trimmed.indexOf("{", jsonBlockStart);
            int jsonEnd = trimmed.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                return trimmed.substring(jsonStart, jsonEnd + 1);
            }
        }

        // ``` ... ``` 블록 추출 (json 키워드 없이)
        int codeBlockStart = trimmed.indexOf("```");
        if (codeBlockStart >= 0) {
            int jsonStart = trimmed.indexOf("{", codeBlockStart);
            int jsonEnd = trimmed.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                return trimmed.substring(jsonStart, jsonEnd + 1);
            }
        }

        // 일반 JSON 객체 추출 시도
        int firstBrace = trimmed.indexOf("{");
        int lastBrace = trimmed.lastIndexOf("}");
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }

        // JSON을 찾지 못하면 원본 반환
        return trimmed;
    }
}
