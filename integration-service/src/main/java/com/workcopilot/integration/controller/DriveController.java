package com.workcopilot.integration.controller;

import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.integration.dto.DriveFileDto;
import com.workcopilot.integration.service.DriveService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integrations/drive")
@RequiredArgsConstructor
public class DriveController {

    private final DriveService driveService;

    @GetMapping("/files")
    public ApiResponse<List<DriveFileDto>> getRecentFiles(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int max) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(driveService.getRecentFiles(userId, max));
    }

    @GetMapping("/files/{fileId}/content")
    public ApiResponse<String> getFileContent(
            Authentication authentication,
            @PathVariable String fileId) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(driveService.getFileContent(userId, fileId));
    }
}
