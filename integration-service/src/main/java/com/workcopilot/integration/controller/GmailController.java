package com.workcopilot.integration.controller;

import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.integration.dto.EmailDto;
import com.workcopilot.integration.service.GmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integrations/gmail")
@RequiredArgsConstructor
public class GmailController {

    private final GmailService gmailService;

    @GetMapping("/messages")
    public ApiResponse<List<EmailDto>> getRecentEmails(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int max) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(gmailService.getRecentEmails(userId, max));
    }

    @GetMapping("/messages/important")
    public ApiResponse<List<EmailDto>> getImportantEmails(
            Authentication authentication,
            @RequestParam(required = false) List<String> domains) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(gmailService.getImportantEmails(userId, domains));
    }

    @GetMapping("/messages/{messageId}")
    public ApiResponse<EmailDto> getEmailById(
            Authentication authentication,
            @PathVariable String messageId) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(gmailService.getEmailById(userId, messageId));
    }
}
