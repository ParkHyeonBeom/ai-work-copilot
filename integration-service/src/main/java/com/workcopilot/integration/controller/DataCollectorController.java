package com.workcopilot.integration.controller;

import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.integration.dto.WorkDataDto;
import com.workcopilot.integration.service.DataCollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations/data")
@RequiredArgsConstructor
public class DataCollectorController {

    private final DataCollectorService dataCollectorService;

    @GetMapping("/collect")
    public ApiResponse<WorkDataDto> collectAll(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(dataCollectorService.collectAll(userId));
    }
}
