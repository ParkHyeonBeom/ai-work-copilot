# 코드 리뷰 스킬

코드를 리뷰할 때 이 체크리스트를 따른다.

## 체크리스트

### 1. 구조
- [ ] 패키지 구조가 `com.workcopilot.{모듈}` 규칙을 따르는가
- [ ] Entity → Repository → Service → Controller 레이어 분리가 되었는가
- [ ] 순환 의존이 없는가 (Service A → Service B → Service A ❌)

### 2. 보안
- [ ] 비밀번호, API 키가 하드코딩되지 않았는가
- [ ] 사용자 입력에 `@Valid` 검증이 있는가
- [ ] 인증이 필요한 API에 Security 설정이 되었는가
- [ ] SQL Injection 위험이 없는가 (네이티브 쿼리 사용 시)

### 3. 성능
- [ ] N+1 쿼리 문제가 없는가 (`@EntityGraph` 또는 fetch join 사용)
- [ ] 불필요한 `@Transactional`이 없는가
- [ ] 읽기 전용은 `@Transactional(readOnly = true)`인가
- [ ] 캐싱이 필요한 곳에 Redis 캐시가 적용되었는가

### 4. 에러 처리
- [ ] `BusinessException(ErrorCode.XXX)`를 사용하는가
- [ ] 적절한 ErrorCode가 정의되었는가
- [ ] 외부 API 호출에 try-catch + 로깅이 있는가

### 5. 코드 품질
- [ ] 메서드가 30줄 이하인가 (초과 시 분리 고려)
- [ ] 매직 넘버/문자열이 상수로 추출되었는가
- [ ] Lombok을 적절히 사용하는가 (`@Setter` 남용 금지)
- [ ] DTO에 record를 사용하는가

## 출력 형식
```
🔍 코드 리뷰 결과
━━━━━━━━━━━━━━━━━━━━
✅ 통과: N개
⚠️ 개선 권장: N개
❌ 수정 필수: N개

[상세 내역]
```
