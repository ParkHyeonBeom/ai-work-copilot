package com.workcopilot.common.exception;

import com.workcopilot.common.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException_ErrorCode기반_응답반환() {
        BusinessException exception = new BusinessException(ErrorCode.USER_NOT_FOUND);

        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("U001");
        assertThat(response.getBody().getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
    }

    @Test
    void handleBusinessException_커스텀메시지_해당메시지반환() {
        BusinessException exception = new BusinessException(ErrorCode.INVALID_INPUT, "이메일 형식이 올바르지 않습니다.");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("C001");
        assertThat(response.getBody().getMessage()).isEqualTo("이메일 형식이 올바르지 않습니다.");
    }

    @Test
    void handleValidationException_필드에러_필드별메시지반환() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "이메일은 필수입니다."));
        bindingResult.addError(new FieldError("request", "name", "이름은 필수입니다."));

        MethodParameter parameter = new MethodParameter(
                this.getClass().getDeclaredMethod("stubMethod", String.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("C001");
        assertThat(response.getBody().getFieldErrors()).containsEntry("email", "이메일은 필수입니다.");
        assertThat(response.getBody().getFieldErrors()).containsEntry("name", "이름은 필수입니다.");
    }

    @Test
    void handleException_일반예외_500에러반환() {
        Exception exception = new RuntimeException("unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("C002");
        assertThat(response.getBody().getMessage()).isEqualTo("서버 내부 오류가 발생했습니다.");
    }

    @SuppressWarnings("unused")
    void stubMethod(String param) {
    }
}
