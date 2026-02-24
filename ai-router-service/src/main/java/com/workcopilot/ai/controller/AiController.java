package com.workcopilot.ai.controller;

import com.workcopilot.ai.dto.AiRequest;
import com.workcopilot.ai.dto.AiResponse;
import com.workcopilot.ai.dto.BriefingRequest;
import com.workcopilot.ai.dto.BriefingResponse;
import com.workcopilot.ai.service.AiService;
import com.workcopilot.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * 일일 업무 브리핑을 생성한다.
     */
    @PostMapping("/briefing")
    public ApiResponse<BriefingResponse> generateBriefing(
            @AuthenticationPrincipal Long userId,
            @RequestBody BriefingRequest request
    ) {
        log.info("브리핑 생성 요청: userId={}", userId);

        // userId를 request에서 가져오되, 인증된 사용자 ID를 우선 사용
        BriefingRequest enrichedRequest = new BriefingRequest(
                userId,
                request.events(),
                request.emails(),
                request.files()
        );

        BriefingResponse response = aiService.generateBriefing(enrichedRequest);
        return ApiResponse.ok(response);
    }

    /**
     * 콘텐츠(이메일 등)를 분류한다.
     */
    @PostMapping("/classify")
    public ApiResponse<AiResponse> classifyContent(@RequestBody AiRequest request) {
        log.info("콘텐츠 분류 요청: taskType={}", request.taskType());
        AiResponse response = aiService.classifyContent(request.content());
        return ApiResponse.ok(response);
    }

    /**
     * 문서를 요약한다.
     */
    @PostMapping("/summarize")
    public ApiResponse<AiResponse> summarizeDocument(@RequestBody AiRequest request) {
        log.info("문서 요약 요청: taskType={}", request.taskType());
        AiResponse response = aiService.summarizeDocument(request.content());
        return ApiResponse.ok(response);
    }
}
