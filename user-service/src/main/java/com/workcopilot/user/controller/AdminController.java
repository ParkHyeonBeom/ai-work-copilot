package com.workcopilot.user.controller;

import com.workcopilot.common.audit.AuditAction;
import com.workcopilot.common.audit.Audited;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.common.dto.ApiResponse;
import com.workcopilot.user.dto.UserResponse;
import com.workcopilot.user.entity.User;
import com.workcopilot.user.entity.UserStatus;
import com.workcopilot.user.repository.UserRepository;
import com.workcopilot.user.service.EmailService;
import com.workcopilot.user.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final VerificationService verificationService;
    private final EmailService emailService;

    @GetMapping("/pending")
    public ApiResponse<List<UserResponse>> getPendingUsers() {
        List<UserResponse> users = userRepository
                .findByStatusOrderByCreatedAtAsc(UserStatus.PENDING_APPROVAL)
                .stream()
                .map(UserResponse::from)
                .toList();
        return ApiResponse.ok(users);
    }

    @PostMapping("/{id}/approve")
    @Audited(action = AuditAction.USER_APPROVED)
    public ApiResponse<UserResponse> approveUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.approve();
        userRepository.save(user);

        String code = verificationService.generateCode(user.getEmail());
        emailService.sendVerificationEmail(user.getEmail(), code);

        log.info("사용자 승인 완료: userId={}, email={}", id, user.getEmail());
        return ApiResponse.ok(UserResponse.from(user));
    }

    @PostMapping("/{id}/reject")
    @Audited(action = AuditAction.USER_REJECTED)
    public ApiResponse<UserResponse> rejectUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.reject();
        userRepository.save(user);

        log.info("사용자 거부: userId={}, email={}", id, user.getEmail());
        return ApiResponse.ok(UserResponse.from(user));
    }
}
