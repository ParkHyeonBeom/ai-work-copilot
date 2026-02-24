package com.workcopilot.briefing.controller;

import com.workcopilot.briefing.dto.BriefingListResponse;
import com.workcopilot.briefing.dto.BriefingResponse;
import com.workcopilot.briefing.service.BriefingService;
import com.workcopilot.briefing.service.BriefingStreamService;
import com.workcopilot.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/briefings")
@RequiredArgsConstructor
public class BriefingController {

    private final BriefingService briefingService;
    private final BriefingStreamService briefingStreamService;

    @PostMapping("/daily")
    public ApiResponse<BriefingResponse> generateDailyBriefing(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("일일 브리핑 생성 요청: userId={}", userId);

        BriefingResponse response = briefingService.generateDailyBriefing(userId);
        return ApiResponse.ok(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<BriefingResponse> getBriefing(@PathVariable Long id,
                                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        BriefingResponse response = briefingService.getBriefing(userId, id);
        return ApiResponse.ok(response);
    }

    @GetMapping("/history")
    public ApiResponse<List<BriefingListResponse>> getBriefingHistory(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<BriefingListResponse> history = briefingService.getBriefingHistory(userId);
        return ApiResponse.ok(history);
    }

    @GetMapping("/today")
    public ApiResponse<BriefingResponse> getTodayBriefing(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return briefingService.getTodayBriefing(userId)
                .map(ApiResponse::ok)
                .orElse(ApiResponse.ok(null, "오늘의 브리핑이 아직 생성되지 않았습니다."));
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBriefing(@PathVariable Long id,
                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("브리핑 스트리밍 요청: userId={}, briefingId={}", userId, id);

        return briefingStreamService.streamBriefing(userId, id);
    }
}
