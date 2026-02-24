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
     * JSON 파싱에 실패하면 원문을 그대로 fullContent로 사용한다.
     */
    private BriefingResponse parseBriefingResponse(String llmResult) {
        try {
            Map<String, Object> resultMap = objectMapper.readValue(llmResult, new TypeReference<>() {});

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
            return new BriefingResponse(
                    "브리핑이 생성되었습니다.",
                    llmResult,
                    List.of(),
                    List.of()
            );
        }
    }
}
