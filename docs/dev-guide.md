# AI Work Copilot — 개발 가이드

> Claude Code에서 `/guide N` 커맨드로 Day별 참조

---

## 로드맵 요약

| Day | 작업 | 상태 |
|-----|------|------|
| 1 | MacBook 환경 검증 + IntelliJ + Claude Code | ✅ 완료 |
| 2 | Windows 서버 (WSL2, K3s, Ollama) | ⏸️ K3s까지 완료, Ollama K8s 배포 미완 |
| 3 | 데이터 인프라 (PostgreSQL, Redis, Kafka, Milvus) | 🔶 Redis K8s 배포 완료, 나머지 미완 |
| 4 | Maven 멀티모듈 + 전체 POM + CLAUDE.md | ✅ 완료 |
| 5 | Claude Code 스킬/에이전트/커맨드 세팅 | ✅ 완료 |
| 6 | 워크플로우 테스트 | ✅ 완료 |
| 7 | common 모듈 보강 | ✅ 완료 |
| 8-9 | user-service (OAuth2, JWT, 온보딩) | ✅ 완료 |
| — | user-service: 관리자 승인 가입 + 이메일 인증 + Audit Logging | ✅ 완료 |
| 10 | integration-service: Google Calendar | ✅ 완료 |
| — | integration-service: 캘린더 일정 생성 + 팀 필터링 + 참석자 알림 | ✅ 완료 |
| 11 | integration-service: Gmail | ✅ 완료 |
| 12 | integration-service: Google Drive + 전체 테스트 | ✅ 완료 |
| 13-14 | ai-router-service (LLM 라우팅, RAG) | ✅ 완료 |
| 15-16 | briefing-service (일일/회의 브리핑, SSE) | ✅ 완료 |
| 17 | gateway | ✅ 완료 |
| 18 | frontend (React 대시보드 + 온보딩 UI) | ✅ 완료 |
| — | frontend: 캘린더 페이지 + 관리자 페이지 + 알림 벨 | ✅ 완료 |
| 19-20 | 프롬프트 튜닝, 캐싱, 비용 최적화 | ⏸️ 프로덕션 환경에서 진행 |
| 21 | 통합 테스트 | ✅ 완료 |
| 22-23 | Docker, Helm, CI/CD, Grafana | ✅ 완료 (Dockerfile + K8s + GitHub Actions CD + ArgoCD) |
| 24-25 | README, API 문서, 데모 | ✅ 완료 (README + Springdoc OpenAPI) |
| — | K8s 배포 안정화 (H2 PVC, Redis, Recreate 전략, 배포 체크리스트) | ✅ 완료 |

### 현재 K8s 배포 현황

| Pod | 상태 | 비고 |
|-----|------|------|
| gateway | ✅ Running | Spring Cloud Gateway, ngrok 연동 |
| user-service | ✅ Running | H2 파일 (PVC) + Redis + Mail |
| integration-service | ✅ Running | H2 파일 (PVC) |
| ai-router-service | ✅ Running | Ollama 미연결 (K8s 배포 필요) |
| briefing-service | ✅ Running | Ollama 미연결 |
| frontend | ✅ Running | React + Vite |
| redis | ✅ Running | 인증코드 저장용 |

### 잔여 작업

| 우선순위 | 작업 | 설명 |
|----------|------|------|
| 🔴 높음 | K8s PostgreSQL 전환 | H2 → PostgreSQL 마이그레이션, k8s 프로필 DB 변경, 무중단 배포(RollingUpdate) 전환 |
| 🔴 높음 | Ollama K8s 배포 | Llama 3.1 8B + nomic-embed-text GPU Pod, ai-router/briefing 연동 |
| 🟡 중간 | Kafka (KRaft) K8s 배포 | 비동기 이벤트 처리 (브리핑 요청 등) |
| 🟡 중간 | Milvus K8s 배포 | RAG 벡터 검색용 |
| 🟡 중간 | 프롬프트 튜닝 + 캐싱 | Ollama 연동 후 LLM 프롬프트 최적화, Redis 캐싱 |
| 🟢 낮음 | Grafana + Prometheus | 모니터링 대시보드 |
| 🟢 낮음 | Google OAuth 앱 인증 | "확인하지 않은 앱" 경고 제거 |
| 🟣 예정 | 채팅 기능 | 사내 실시간 채팅 (WebSocket 기반) |

---

## Day 7: common 모듈 보강

### 목표
GlobalExceptionHandler, 유틸리티 클래스 추가

### 작업
1. `common/exception/GlobalExceptionHandler.java` — 전역 예외 처리
   - BusinessException → ErrorCode 기반 응답
   - MethodArgumentNotValidException → 필드별 에러 메시지
   - Exception → 500 Internal Server Error
2. `common/util/DateTimeUtil.java` — 날짜/시간 유틸
3. `common/config/JpaConfig.java` — JPA Auditing 설정 (공통)
4. common 모듈 단위 테스트

---

## Day 8-9: user-service

### 목표
Google OAuth2 로그인 → JWT 발급 → 사용자 관리 → 온보딩 API

### Day 8 작업

1. **User 엔티티**
```
user-service/src/main/java/com/workcopilot/user/entity/User.java
- BaseEntity 상속
- email (unique, not null)
- name
- profileImageUrl
- googleId (unique)
- role (enum: USER, ADMIN)
- onboardingCompleted (boolean, default false)
- settings (JSON → UserSettings)
```

2. **UserSettings (JSON 필드)**
```
user-service/src/main/java/com/workcopilot/user/entity/UserSettings.java
- monitoredCalendarIds (List<String>, 기본: ["primary"])
- monitoredDriveFolderIds (List<String>, 기본: ["root"])
- importantDomains (List<String>)
- excludeLabels (List<String>, 기본: ["PROMOTIONS", "SOCIAL"])
- workStartTime (String, 기본: "09:00")
- workEndTime (String, 기본: "18:00")
- language (String, 기본: "ko")
- timezone (String, 기본: "Asia/Seoul")
- static defaults() 팩토리 메서드
```

3. **UserRepository**
```
findByEmail(String email) → Optional<User>
findByGoogleId(String googleId) → Optional<User>
existsByEmail(String email) → boolean
```

4. **SecurityConfig**
```
- OAuth2 로그인 설정
- JWT 필터 등록
- 공개 경로: /api/auth/**, /login/oauth2/**, /h2-console/**
- 나머지: 인증 필요
- CORS 설정
- CSRF 비활성화 (REST API)
```

5. **JwtProvider**
```
- generateAccessToken(User user) → String
- generateRefreshToken(User user) → String
- validateToken(String token) → boolean
- getUserIdFromToken(String token) → Long
- 설정: jwt.secret, jwt.access-token-expiry, jwt.refresh-token-expiry
```

### Day 9 작업

6. **OAuth2SuccessHandler**
```
- Google 로그인 성공 시 호출
- User 엔티티 조회 또는 신규 생성
- Access Token + Refresh Token 발급
- 프론트엔드로 리다이렉트 (토큰 포함)
```

7. **JwtAuthenticationFilter**
```
- OncePerRequestFilter 상속
- Authorization: Bearer {token} 헤더에서 토큰 추출
- 토큰 검증 → SecurityContext에 인증 정보 설정
```

8. **UserService + UserController**
```
GET  /api/users/me          → 현재 로그인 사용자 정보
PUT  /api/users/me/settings  → 설정 변경
POST /api/users/me/onboarding → 온보딩 완료
```

9. **DTO (record)**
```
UserResponse, UpdateSettingsRequest, OnboardingRequest
```

10. **테스트**
```
UserServiceTest, JwtProviderTest, UserControllerTest
```

---

## Day 10: integration-service — Google Calendar

### 목표
Google Calendar API 연동, 일정 조회

### 작업
1. `GoogleTokenService` — Access Token 관리 (갱신 포함)
2. `CalendarService` — Calendar API 호출
   - getUpcomingEvents(userId, days) → 향후 N일 일정
   - getTodayEvents(userId) → 오늘 일정
   - getEventById(userId, eventId) → 단건 조회
3. `CalendarEventDto` — 일정 DTO (시작/종료 시간, 제목, 참석자, 위치)
4. `CalendarController`
   - GET /api/integrations/calendar/events?days=7
   - GET /api/integrations/calendar/events/today
5. 테스트 (Mock Google API)

---

## Day 11: integration-service — Gmail

### 목표
Gmail API 연동, 이메일 조회 + 요약

### 작업
1. `GmailService`
   - getRecentEmails(userId, maxResults) → 최근 이메일 목록
   - getImportantEmails(userId) → 중요 이메일 (importantDomains 필터)
   - getEmailById(userId, messageId) → 단건 (본문 포함)
2. `EmailDto` — 이메일 DTO (발신자, 제목, 본문 snippet, 날짜, 라벨)
3. `GmailController`
   - GET /api/integrations/gmail/messages?max=20
   - GET /api/integrations/gmail/messages/important
4. 테스트

---

## Day 12: integration-service — Google Drive + 통합

### 목표
Google Drive API 연동, 최근 문서 조회

### 작업
1. `DriveService`
   - getRecentFiles(userId, maxResults) → 최근 수정된 파일
   - getFileContent(userId, fileId) → 파일 내용 (텍스트 추출)
2. `DriveFileDto` — 파일 DTO (이름, MIME 타입, 수정 시간, 소유자)
3. `DriveController`
   - GET /api/integrations/drive/files?max=20
4. integration-service 전체 통합 테스트
5. `DataCollectorService` — Calendar + Gmail + Drive 데이터를 종합 수집하는 퍼사드

---

## Day 13-14: ai-router-service

### 목표
LLM 라우팅, 임베딩 생성, RAG 파이프라인

### Day 13 작업
1. `LlmRouter` — 작업 유형별 모델 선택
   - CLASSIFICATION → Ollama (Llama 3.1 8B)
   - BRIEFING → OpenAI (GPT-4o)
   - SUMMARIZATION → Anthropic (Claude Sonnet)
2. `LlmService` — Spring AI ChatModel 래핑
   - chat(prompt, modelType) → String
   - chatWithFunctions(prompt, functions) → StructuredResponse
3. `EmbeddingService` — nomic-embed-text로 임베딩 생성
4. Config: 각 모델별 Spring AI 설정

### Day 14 작업
5. `VectorStoreService` — Milvus 연동
   - store(documents) → 벡터 저장
   - search(query, topK) → 유사 문서 검색
6. `RagService` — RAG 파이프라인
   - 질문 → 임베딩 → 유사 문서 검색 → 컨텍스트와 함께 LLM 호출
7. `AiController`
   - POST /api/ai/chat
   - POST /api/ai/classify
   - POST /api/ai/summarize
8. 테스트 (Mock LLM 응답)

---

## Day 15-16: briefing-service

### 목표
일일 브리핑 생성, SSE 스트리밍

### Day 15 작업
1. `Briefing` 엔티티 (BaseEntity 상속)
   - userId, briefingType (DAILY/MEETING/ADHOC)
   - content (TEXT), summary
   - status (PENDING/GENERATING/COMPLETED/FAILED)
2. `BriefingRepository`
3. `BriefingGenerator` — 데이터 수집 → LLM 호출 → 브리핑 생성
   - collectData(userId) → integration-service 호출
   - generateBriefing(data) → ai-router-service 호출
   - saveBriefing(briefing) → DB 저장
4. Kafka Consumer — BriefingRequestedEvent 수신 시 브리핑 생성

### Day 16 작업
5. `SseService` — Server-Sent Events 스트리밍
   - 브리핑 생성 중 실시간 진행 상황 전송
   - LLM 응답을 토큰 단위로 스트리밍
6. `BriefingController`
   - POST /api/briefings/daily → 일일 브리핑 요청
   - GET /api/briefings/{id} → 브리핑 조회
   - GET /api/briefings/{id}/stream → SSE 스트리밍
   - GET /api/briefings/history → 브리핑 이력
7. 테스트

---

## Day 17: gateway

### 목표
API Gateway 설정, JWT 검증 필터, Rate Limiting

### 작업
1. `JwtAuthGatewayFilter` — Gateway에서 JWT 검증
2. `RateLimitConfig` — Redis 기반 Rate Limiting
3. `CorsConfig` — CORS 설정
4. Route 설정 검증 (각 서비스로 정상 라우팅 확인)
5. 테스트

---

## Day 18: frontend

### 목표
React 대시보드 + 온보딩 UI

### 작업
1. Vite + React 18 + TailwindCSS 4 프로젝트 생성
2. 페이지:
   - `/login` — Google 로그인 버튼
   - `/onboarding` — 설정 선택 (캘린더, 근무 시간 등)
   - `/dashboard` — 오늘의 브리핑 + 일정 + 이메일 요약
   - `/briefing/:id` — 브리핑 상세 (SSE 스트리밍)
3. 컴포넌트: Header, BriefingCard, CalendarWidget, EmailList
4. API 연동: axios + JWT 토큰 관리

---

## Day 19-20: AI 레이어 최적화

### 작업
1. 프롬프트 튜닝 — 브리핑 품질 개선
2. 응답 캐싱 — Redis에 동일 요청 캐싱
3. 비용 최적화 — 로컬 LLM 활용 극대화
4. 프롬프트 템플릿 관리

---

## Day 21: 통합 테스트

### 작업
1. 전체 플로우 테스트: 로그인 → 데이터 수집 → 브리핑 생성
2. 서비스 간 통신 테스트
3. 에러 시나리오 테스트

---

## Day 22-23: 인프라

### 작업
1. 각 서비스 Dockerfile 작성
2. Helm Chart 작성
3. GitHub Actions CI/CD
4. Grafana + Prometheus 모니터링

---

## Day 24-25: 마무리

### 작업
1. README.md 작성 (프로젝트 소개, 아키텍처, 실행 방법)
2. API 문서 정리 (api-documenter 에이전트 활용)
3. 데모 시나리오 준비 + 스크린샷
4. GitHub 레포 정리 (라벨, 이슈, 마일스톤)
