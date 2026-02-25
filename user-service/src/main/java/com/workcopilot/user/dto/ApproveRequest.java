package com.workcopilot.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveRequest(
        @NotBlank(message = "직급은 필수입니다") String position,
        @NotBlank(message = "부서는 필수입니다") String department,
        String internalPhone
) {
}
