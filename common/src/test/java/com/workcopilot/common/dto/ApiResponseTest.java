package com.workcopilot.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void ok_데이터전달_성공응답() {
        ApiResponse<String> response = ApiResponse.ok("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void ok_데이터와메시지_성공응답() {
        ApiResponse<Integer> response = ApiResponse.ok(42, "조회 성공");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(42);
        assertThat(response.getMessage()).isEqualTo("조회 성공");
    }

    @Test
    void error_에러정보_실패응답() {
        ApiResponse<Void> response = ApiResponse.error("사용자를 찾을 수 없습니다.", "U001");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
        assertThat(response.getErrorCode()).isEqualTo("U001");
        assertThat(response.getTimestamp()).isNotNull();
    }
}
