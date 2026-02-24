package com.workcopilot.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Gateway 전역 에러 핸들러.
 * 라우팅 에러, JWT 검증 에러 등을 JSON 형태로 반환한다.
 *
 * 응답 형식:
 * <pre>
 * {
 *   "success": false,
 *   "message": "에러 메시지",
 *   "code": "에러 코드"
 * }
 * </pre>
 */
@Slf4j
@Component
@Order(-2) // DefaultErrorWebExceptionHandler(-1)보다 높은 우선순위
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties webProperties,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        HttpStatus status = determineHttpStatus(error);
        String code = determineErrorCode(status);
        String message = determineMessage(error, status);

        log.error("Gateway 에러 발생 - status: {}, path: {}, message: {}",
                status.value(), request.path(), message);

        Map<String, Object> body = Map.of(
                "success", false,
                "message", message,
                "code", code
        );

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body));
    }

    private HttpStatus determineHttpStatus(Throwable error) {
        if (error instanceof ResponseStatusException responseStatusException) {
            return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String determineErrorCode(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE";
            case BAD_GATEWAY -> "BAD_GATEWAY";
            case GATEWAY_TIMEOUT -> "GATEWAY_TIMEOUT";
            case TOO_MANY_REQUESTS -> "TOO_MANY_REQUESTS";
            default -> "INTERNAL_SERVER_ERROR";
        };
    }

    private String determineMessage(Throwable error, HttpStatus status) {
        if (error instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getReason() != null
                    ? responseStatusException.getReason()
                    : status.getReasonPhrase();
        }

        return switch (status) {
            case NOT_FOUND -> "요청한 리소스를 찾을 수 없습니다.";
            case SERVICE_UNAVAILABLE -> "서비스를 사용할 수 없습니다. 잠시 후 다시 시도해주세요.";
            case BAD_GATEWAY -> "업스트림 서비스에 연결할 수 없습니다.";
            case GATEWAY_TIMEOUT -> "업스트림 서비스 응답 시간이 초과되었습니다.";
            default -> "서버 내부 오류가 발생했습니다.";
        };
    }
}
