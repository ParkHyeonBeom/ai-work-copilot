# REST API 생성 스킬

## URL 설계 규칙
- 복수형 명사: `/api/users`, `/api/briefings`
- 하위 리소스: `/api/users/{id}/settings`
- 동사 금지: `/api/users/create` ❌ → `POST /api/users` ✅
- 케밥 케이스: `/api/briefing-requests`

## HTTP 메서드
| 메서드 | 용도 | 응답 코드 |
|--------|------|----------|
| GET | 조회 | 200 |
| POST | 생성 | 201 |
| PUT | 전체 수정 | 200 |
| PATCH | 부분 수정 | 200 |
| DELETE | 삭제 | 204 |

## 응답 형식
모든 API는 `ApiResponse<T>`로 감싼다:
```java
// 성공
@PostMapping
public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
    UserResponse result = userService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result, "사용자 생성 완료"));
}

// 목록 조회
@GetMapping
public ResponseEntity<ApiResponse<List<UserResponse>>> findAll() {
    return ResponseEntity.ok(ApiResponse.ok(userService.findAll()));
}
```

## 전역 예외 처리
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus())
            .body(ApiResponse.error(e.getMessage(), code.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException e) {
        // @Valid 검증 실패 시
    }
}
```
