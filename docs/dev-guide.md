# AI Work Copilot — 개발 가이드

> Claude Code에서 `/guide N` 커맨드로 Day별 참조
> `/daily` 커맨드로 현재 진행 상황 파악

---

## 로드맵 요약

### Phase 1: 기반 구축 (Day 1~6)

| Day | 작업 | 상태 |
|-----|------|------|
| 1 | MacBook 환경 검증 + IntelliJ + Claude Code | ✅ 완료 |
| 2 | Windows 서버 (WSL2, K3s, Ollama) | ✅ 완료 (Ollama K8s 배포 미완) |
| 3 | 데이터 인프라 (Redis K8s 배포) | ✅ 완료 (Kafka/Milvus 미완) |
| 4 | Maven 멀티모듈 + 전체 POM + CLAUDE.md | ✅ 완료 |
| 5 | Claude Code 스킬/에이전트/커맨드 세팅 | ✅ 완료 |
| 6 | 워크플로우 테스트 | ✅ 완료 |

### Phase 2: 핵심 서비스 (Day 7~18)

| Day | 작업 | 상태 |
|-----|------|------|
| 7 | common 모듈 보강 | ✅ 완료 |
| 8-9 | user-service (OAuth2, JWT, 온보딩, 관리자 승인, 이메일 인증, Audit) | ✅ 완료 |
| 10 | integration-service: Google Calendar + 일정 생성 + 팀 필터링 | ✅ 완료 |
| 11 | integration-service: Gmail | ✅ 완료 |
| 12 | integration-service: Google Drive + 통합 테스트 | ✅ 완료 |
| 13-14 | ai-router-service (LLM 라우팅, RAG) | ✅ 완료 |
| 15-16 | briefing-service (일일/회의 브리핑, SSE) | ✅ 완료 |
| 17 | gateway (Spring Cloud Gateway) | ✅ 완료 |
| 18 | frontend (React 대시보드 + 온보딩 + 캘린더 + 관리자) | ✅ 완료 |

### Phase 3: 안정화 + CI/CD (Day 19~25)

| Day | 작업 | 상태 |
|-----|------|------|
| 19-20 | 프롬프트 튜닝, 캐싱, 비용 최적화 | ⏸️ 프로덕션 환경에서 진행 |
| 21 | 통합 테스트 | ✅ 완료 |
| 22-23 | Dockerfile + K8s + GitHub Actions CD | ✅ 완료 |
| 24-25 | README + Springdoc OpenAPI + 배포 체크리스트 | ✅ 완료 |

### Phase 4: 인프라 전환 + 신규 기능

| 작업 | 상태 |
|------|------|
| H2/PostgreSQL → MySQL 8.0 통일 + Docker Compose 로컬 환경 | ✅ 완료 |
| Gemini LLM 연동 (gemini-2.5-flash) + 브리핑 프롬프트 개선 | ✅ 완료 |
| 캘린더 이중 운영 (Google Calendar + Local DB) | ✅ 완료 |

### Phase 5: chat-service + AI 에이전트

| 작업 | 상태 |
|------|------|
| chat-service 구축 (WebSocket STOMP + REST API) | ✅ 완료 |
| AI 에이전트 챗봇 (다중턴 대화 + 컨텍스트 수집) | ✅ 완료 |
| chat-service K8s 배포 + CD 파이프라인 추가 | ✅ 완료 |

### Phase 6: 채팅 고도화

| 작업 | 상태 |
|------|------|
| Phase A: WebSocket Context Provider, Toast 알림, 날짜 구분선, Load More, 타이핑 표시 | ✅ 완료 |
| Phase B: 메시지 삭제, 답장(Reply), 이미지 미리보기, 실시간 Unread, 브라우저 Push 알림 | ✅ 완료 |
| Phase C: 메시지 수정, 리액션, D&D 파일, 검색, Presence, @멘션 | 🟣 예정 |

---

## 모듈 구조

```
common/               → 공통 (ApiResponse, BaseEntity, ErrorCode, AuditLog)
user-service/          → OAuth2 + JWT + 관리자 승인 + 이메일 인증 (포트 8081)
integration-service/   → Google Calendar + Gmail + Drive (포트 8082)
ai-router-service/     → LLM 라우팅 + RAG + AI 에이전트 (포트 8083)
briefing-service/      → 일일 브리핑, SSE 스트리밍 (포트 8084)
chat-service/          → 실시간 채팅 (WebSocket STOMP + REST) (포트 8085)
gateway/              → Spring Cloud Gateway (포트 8080)
frontend/             → React 18 + Vite + TailwindCSS 4 (포트 5173)
```

---

## 현재 K8s 배포 현황

| Pod | 상태 | 비고 |
|-----|------|------|
| gateway | ✅ Running | Spring Cloud Gateway, ngrok 연동 |
| user-service | ✅ Running | MySQL + Redis + Mail |
| integration-service | ✅ Running | MySQL + Google API |
| ai-router-service | ✅ Running | Gemini 연동 (GEMINI_API_KEY 필요) |
| briefing-service | ✅ Running | Gemini 연동 |
| chat-service | ✅ Running | MySQL + WebSocket STOMP |
| frontend | ✅ Running | React + Vite |
| mysql | ✅ Running | 5개 DB (workcopilot, _integration, _ai, _briefing, _chat) |
| redis | ✅ Running | 인증코드 저장용 |

---

## 잔여 작업

| 우선순위 | 작업 | 설명 |
|----------|------|------|
| 🔴 높음 | Ollama K8s 배포 | Llama 3.1 8B + nomic-embed-text GPU Pod, Gemini 폴백용 |
| 🟡 중간 | 채팅 Phase C | 메시지 수정, 리액션, D&D 파일, 검색, Presence, @멘션 |
| 🟡 중간 | Kafka (KRaft) K8s 배포 | 비동기 이벤트 처리 (브리핑 요청 등) |
| 🟡 중간 | Milvus K8s 배포 | RAG 벡터 검색용 |
| 🟡 중간 | 프롬프트 튜닝 + 캐싱 | LLM 프롬프트 최적화, Redis 캐싱 |
| 🟡 중간 | Docker Compose Watch / 핫리로드 | 개발 시 수동 재빌드 자동화 |
| 🟢 낮음 | Grafana + Prometheus | 모니터링 대시보드 |
| 🟢 낮음 | Google OAuth 앱 인증 | "확인하지 않은 앱" 경고 제거 |

---

## DB 구성 (MySQL 8.0)

H2/PostgreSQL에서 MySQL 8.0으로 통일 완료. 서비스별 DB 분리.

| DB | 서비스 | 주요 테이블 |
|----|--------|-------------|
| workcopilot | user-service | users, audit_logs |
| workcopilot_integration | integration-service | calendar_events |
| workcopilot_ai | ai-router-service | conversation_histories, conversation_messages |
| workcopilot_briefing | briefing-service | briefings |
| workcopilot_chat | chat-service | chat_rooms, chat_messages, chat_participants, chat_files |

- 초기화: `scripts/init-databases.sql`
- Docker Compose: MySQL 컨테이너가 자동 초기화

---

## Docker Compose 로컬 개발

```bash
# 전체 서비스 빌드 + 실행
mvn clean package -DskipTests && docker compose up --build

# 특정 서비스만 재빌드
mvn package -pl :chat-service -am -DskipTests && docker compose up -d --build chat-service

# 로그 확인
docker compose logs -f chat-service
docker compose logs -f gateway
```

| 서비스 | 로컬 포트 | 컨테이너 포트 |
|--------|-----------|---------------|
| MySQL | 3306 | 3306 |
| Redis | 6379 | 6379 |
| user-service | 8081 | 8081 |
| integration-service | 8082 | 8082 |
| ai-router-service | 8083 | 8083 |
| briefing-service | 8084 | 8084 |
| chat-service | 8085 | 8085 |
| gateway | 8080 | 8080 |
| frontend | 5173 | 80 |

---

## LLM 라우팅

| 작업 | 모델 | 이유 |
|------|------|------|
| AI 에이전트 대화 | Gemini 2.5 Flash → Ollama 폴백 | 빠른 응답 + 비용 절감 |
| 종합 브리핑 | Gemini 2.5 Flash → Ollama 폴백 | 정확도 우선 |
| 텍스트 분류/키워드 | Ollama Llama 3.1 8B | Self-hosted, 비용 절감 |

환경변수: `GEMINI_ENABLED=true`, `GEMINI_API_KEY=your-key`

---

## chat-service 상세

### 엔티티
- **ChatRoom**: name, type (DIRECT/GROUP/GENERAL), lastMessageContent/At
- **ChatParticipant**: userId, userName, lastReadMessageId (읽음 추적)
- **ChatMessage**: type (TEXT/FILE), content, deleted, replyToMessageId
- **ChatFile**: originalFileName, mimeType, fileSize

### WebSocket (STOMP)
```
엔드포인트: /ws/chat
애플리케이션 프리픽스: /app
브로커: /topic (broadcast), /queue (private)
사용자 프리픽스: /user

메시지 전송: /app/chat.send/{roomId}     → /topic/room/{roomId}
타이핑 표시: /app/chat.typing/{roomId}   → /topic/room/{roomId}/typing
메시지 삭제: /app/chat.delete/{roomId}   → /topic/room/{roomId}
알림:       서버 → /user/{userId}/queue/notifications
```

### REST API
```
POST   /api/chat/rooms                          → 채팅방 생성
GET    /api/chat/rooms                          → 내 채팅방 목록
GET    /api/chat/rooms/{roomId}                 → 채팅방 상세
DELETE /api/chat/rooms/{roomId}                 → 채팅방 나가기
POST   /api/chat/rooms/{roomId}/invite          → 멤버 초대
GET    /api/chat/rooms/{roomId}/messages         → 메시지 히스토리 (cursor 페이징)
DELETE /api/chat/rooms/{roomId}/messages/{msgId}  → 메시지 삭제
POST   /api/chat/rooms/{roomId}/read             → 읽음 처리
GET    /api/chat/unread-count                    → 전체 미읽음 수
POST   /api/chat/rooms/{roomId}/files/upload     → 파일 업로드
GET    /api/chat/files/{fileId}/download          → 파일 다운로드
```

### 고도화 기능 (Phase A+B 완료)
- WebSocket Context Provider (글로벌 연결 공유)
- Toast 알림 (다른 페이지에서 채팅 수신 시)
- 날짜 구분선 (오늘/어제/전체 날짜)
- 이전 메시지 로드 (cursor 페이징 + 스크롤 위치 유지)
- 타이핑 인디케이터 (2초 자동 해제)
- 메시지 삭제 (soft delete + WebSocket 브로드캐스트)
- 답장/인용 (replyToMessageId + ReplyMessageDto)
- 이미지 인라인 미리보기 (MIME 타입 감지)
- 실시간 안읽은 수 업데이트 (WebSocket → Layout 배지)
- 브라우저 Push 알림 (탭 비활성 시)

---

## AI 에이전트 상세

### 개요
다중턴 대화 기반 AI 어시스턴트. 사용자의 캘린더/이메일/드라이브/채팅 데이터를 병렬 수집하여 컨텍스트 기반 응답 생성.

### 아키텍처
```
사용자 질문
  ↓
AgentService.chat()
  ├→ AgentContextCollector (병렬 4스레드)
  │   ├→ Calendar: /api/integrations/calendar/events?days=2
  │   ├→ Email: /api/integrations/gmail/messages/recent?max=10
  │   ├→ Drive: /api/integrations/drive/files?max=5
  │   └→ Chat: ChatServiceClient → /api/chat/rooms
  ├→ AgentContextPreprocessor (관련성 필터링, 3000자/소스, 총 12000자)
  ├→ AgentPromptBuilder (시스템 + 컨텍스트 + 히스토리 + 질문)
  └→ LlmRouter.route("agent", prompt) → Gemini / Ollama
```

### 엔티티
- **ConversationHistory**: userId, title (첫 메시지 자동), isActive
- **ConversationMessage**: role (user/assistant), content, contextSources, model, processingTimeMs

### REST API
```
POST   /api/ai/agent/chat                    → 메시지 전송
GET    /api/ai/agent/conversations            → 대화 목록
GET    /api/ai/agent/conversations/{id}       → 대화 상세 (메시지 포함)
DELETE /api/ai/agent/conversations/{id}       → 대화 삭제 (soft)
```

---

## Frontend 현황

### 페이지 구조
```
/login              → Google OAuth 로그인
/onboarding         → 초기 설정
/dashboard          → 오늘의 브리핑 + 일정 + 이메일
/briefings          → 브리핑 목록
/briefing/:id       → 브리핑 상세 (SSE)
/calendar           → 캘린더 (Google + Local)
/chat               → 채팅방 목록 + 메시지
/chat/:roomId       → 특정 채팅방
/agent              → AI 어시스턴트
/agent/:convId      → 특정 대화
/admin              → 사용자 관리 (관리자 전용)
```

### 핵심 아키텍처
- **WebSocketContext**: STOMP 클라이언트 글로벌 관리 (자동 재연결, 알림 구독)
- **ChatToast**: WebSocket 알림 수신 → Toast + 브라우저 Push
- **useWebSocket**: Context 래퍼 훅 (subscribe/publish/addNotificationListener)

### 주요 컴포넌트
| 영역 | 컴포넌트 |
|------|---------|
| 레이아웃 | Layout, Header |
| 채팅 | ChatPage, ChatRoomList, ChatMessageArea, ChatInput, ChatCreateModal, ChatToast |
| 에이전트 | AgentPage, AgentSidebar, AgentChatArea, AgentInput |
| 대시보드 | DashboardPage, BriefingCard, CalendarWidget, EmailList |
| 캘린더 | CalendarPage, EventCreateModal |

---

## Day별 상세 가이드

### Day 7: common 모듈 보강

**목표**: GlobalExceptionHandler, 유틸리티 클래스 추가

1. `GlobalExceptionHandler.java` — BusinessException, Validation, 500 에러 처리
2. `DateTimeUtil.java` — 날짜/시간 유틸
3. `JpaConfig.java` — JPA Auditing 공통 설정
4. common 모듈 단위 테스트

---

### Day 8-9: user-service

**목표**: Google OAuth2 로그인 → JWT → 사용자 관리 → 온보딩 → 관리자 승인 → 이메일 인증

- User 엔티티 (BaseEntity, email, googleId, role, status, settings JSON)
- SecurityConfig, JwtProvider, OAuth2SuccessHandler, JwtAuthenticationFilter
- UserService + UserController (me, settings, onboarding)
- 관리자 승인 가입 플로우 (PENDING_APPROVAL → ACTIVE)
- Redis 기반 이메일 인증 코드
- AOP Audit Logging (@Audited)

---

### Day 10-12: integration-service

**목표**: Google Calendar + Gmail + Drive API 연동

- GoogleTokenService (Access Token 관리/갱신)
- CalendarService + CalendarController (일정 조회/생성, 팀 필터링)
- GmailService + GmailController (이메일 조회, 중요 필터)
- DriveService + DriveController (파일 조회/내용 추출)
- DataCollectorService (Calendar + Gmail + Drive 종합 수집)
- CalendarSource enum (GOOGLE/LOCAL/BOTH) — 캘린더 이중 운영

---

### Day 13-14: ai-router-service

**목표**: LLM 라우팅 + RAG 파이프라인

- LlmRouter (작업별 모델 선택: Gemini → Ollama 폴백)
- LlmService (Spring AI ChatModel 래핑)
- EmbeddingService (nomic-embed-text)
- VectorStoreService (Milvus 연동 예정)
- AiController (chat, classify, summarize)

---

### Day 15-16: briefing-service

**목표**: 일일 브리핑 생성 + SSE 스트리밍

- Briefing 엔티티 (DAILY/MEETING/ADHOC, 상태 관리)
- BriefingGenerator (데이터 수집 → LLM 호출 → 저장)
- SseService (Server-Sent Events 실시간 스트리밍)
- BriefingController (daily, 조회, stream, history)

---

### Day 17: gateway

**목표**: API Gateway + JWT 검증 + CORS

- JwtAuthGatewayFilter, CorsConfig
- Route 설정 (user/integration/ai/briefing/chat + WebSocket)

---

### Day 18: frontend

**목표**: React 대시보드 + 온보딩 + 캘린더 + 관리자 페이지

- Vite + React 18 + TailwindCSS 4
- 페이지: Login, Onboarding, Dashboard, Briefing, Calendar, Admin
- axios + JWT 토큰 관리, 다크모드

---

### Day 22-23: CI/CD + 인프라

- 각 서비스 Dockerfile (멀티스테이지 빌드)
- K8s manifests (deployment + service)
- GitHub Actions CD (main push → 빌드 → Docker 이미지 → K8s 태그 갱신)
- kustomization.yaml로 이미지 태그 관리

---

### Phase 4: MySQL 통일 + Gemini

- H2/PostgreSQL → MySQL 8.0 전환 (5개 분리 DB)
- Docker Compose 로컬 개발 환경 구축
- `scripts/init-databases.sql` 자동 초기화
- Gemini 2.5 Flash LLM 연동 (GEMINI_API_KEY)
- EmailDto isRead 필드 추가
- 브리핑 프롬프트 개선

---

### Phase 5: chat-service + AI 에이전트

- chat-service 모듈 신규 구축 (엔티티, WebSocket, REST, 파일 업로드)
- AI 에이전트 (AgentService, AgentContextCollector, AgentPromptBuilder)
- ConversationHistory/Message 엔티티
- K8s deployment + service + CD 파이프라인 추가
- Gateway 라우트 추가 (REST + WebSocket)
- Frontend: ChatPage, AgentPage + 관련 컴포넌트

---

### Phase 6: 채팅 고도화

**Phase A (핵심 UX):**
- WebSocket Context Provider (글로벌 연결)
- ChatToast (다른 페이지 알림)
- 날짜 구분선, Load More, 타이핑 인디케이터

**Phase B (메시지 기능):**
- 메시지 삭제 (soft delete + WebSocket 이벤트)
- 답장/인용 (replyToMessageId)
- 이미지 인라인 미리보기
- 실시간 Unread 업데이트 (Layout 배지)
- 브라우저 Push 알림

**Phase C (예정):**
- 메시지 수정 (editedAt, 5분 제한)
- 리액션/이모지 (ChatReaction 엔티티)
- 드래그 앤 드롭 파일 업로드
- 메시지 검색
- 접속 상태 (Presence)
- @멘션
