# AI Work Copilot - Claude Code 규칙

## 프로젝트
Google Calendar + Gmail + Drive를 분석하여 AI 업무 브리핑을 제공하는 서비스.

## 기술 스택
- Java 21, Spring Boot 3.3, Spring AI 1.0, Maven 멀티모듈
- PostgreSQL 16, Redis 7, Kafka, Milvus 2.4
- React 18, Vite, TailwindCSS 4

## 코딩 컨벤션
- 패키지: `com.workcopilot.{모듈명}`
- DTO: record 사용 권장
- 에러: `BusinessException(ErrorCode.XXX)` throw
- API 응답: `ApiResponse.ok(data)` 또는 `ApiResponse.error(msg, code)`
- 엔티티: `BaseEntity` 상속 (id, createdAt, updatedAt)
- 테스트: 단위 테스트 필수, `@SpringBootTest`는 통합 테스트에만

## 포트
- 8080: Gateway
- 8081: user-service
- 8082: integration-service
- 8083: ai-router-service
- 8084: briefing-service

## 프로파일
- local: H2 인메모리 (개발), Redis/Kafka 없이도 동작
- prod: PostgreSQL + Redis + Kafka + Milvus

## 가이드
상세 구현 가이드: `docs/dev-guide.md` → `/guide N` 커맨드로 Day별 참조
