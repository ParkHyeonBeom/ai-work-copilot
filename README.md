# WorkBridge — 사내 업무 통합 플랫폼

Google Calendar + Gmail + Drive 데이터를 AI(Gemini/Ollama)로 분석하여 일일 업무 브리핑을 자동 생성하는 사내용 솔루션.
관리자 승인 기반 접근 제어, Audit Logging, 실시간 채팅, AI Agent 등 엔터프라이즈급 기능을 갖춘 마이크로서비스 플랫폼.

> **Claude Code 기반 AI Native 개발** — 설계부터 배포까지 전 과정을 Claude Code와 협업하여 진행한 프로젝트입니다.

---

## Architecture

```
                         ┌─────────────────────┐
                         │   React Frontend    │
                         │   (Vite + TailwindCSS)│
                         └──────────┬──────────┘
                                    │
                         ┌──────────▼──────────┐
                         │  API Gateway :8080   │
                         │ (Spring Cloud Gateway)│
                         │   JWT Validation     │
                         └──┬───┬───┬───┬───┬──┘
                            │   │   │   │   │
              ┌─────────────┘   │   │   │   └─────────────┐
              │                 │   │   │                  │
     ┌────────▼───────┐ ┌──────▼───▼──────┐ ┌─────────────▼──────┐
     │  User Service  │ │  Integration    │ │  Briefing Service  │
     │    :8081       │ │  Service :8082  │ │      :8084         │
     │                │ │                 │ │                    │
     │ OAuth2 + JWT   │ │ Google Calendar │ │ Daily Briefing     │
     │ Admin Approval │ │ Gmail + Drive   │ │ SSE Streaming      │
     │ Email Verify   │ │ Token Refresh   │ │                    │
     │ Audit Logging  │ │                 │ │                    │
     └────────┬───────┘ └────────┬────────┘ └────────┬───────────┘
              │                  │                    │
              │          ┌───────▼────────┐           │
              │          │  AI Router     │◄──────────┘
              │          │  Service :8083 │
              │          │                │
              │          │ Gemini 2.5 Flash│
              │          │ Ollama Fallback │
              │          │ AI Agent       │
              │          └───────┬────────┘
              │                  │
     ┌────────▼───────┐ ┌───────▼────────┐ ┌──────────────────┐
     │  Chat Service  │ │   MySQL 8.0    │ │     Redis 7      │
     │    :8085       │ │   (5 DBs)      │ │  (Cache + Auth)  │
     │                │ │                │ │                  │
     │ WebSocket STOMP│ └────────────────┘ └──────────────────┘
     │ Real-time Chat │
     │ File Upload    │
     │ @Mention       │
     │ Reactions      │
     └────────────────┘

              ┌─────────────────────────────────────────┐
              │         K3s Cluster (Home Server)       │
              │   ArgoCD GitOps + Tailscale VPN + ngrok │
              └─────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.6, Spring Cloud 2023.0.4 |
| API Gateway | Spring Cloud Gateway + JWT Filter |
| Database | MySQL 8.0 (5개 서비스별 DB 분리) |
| Cache | Redis 7 (인증 코드, 세션 캐시) |
| LLM | Gemini 2.5 Flash + Ollama Llama 3.1 8B (Self-hosted) |
| Real-time | WebSocket STOMP (채팅), SSE (브리핑 스트리밍) |
| Frontend | React 18, Vite, TailwindCSS 4 |
| Infra | K3s (단일 노드), Docker, Tailscale VPN, ngrok |
| CI/CD | GitHub Actions + ArgoCD (GitOps 자동 배포) |
| Registry | GitHub Container Registry (GHCR) |
| AI Dev Tool | Claude Code (CLAUDE.md + Commands + Skills + Agents) |

---

## Modules

```
ai-work-copilot/
├── common/                  # 공통: ApiResponse, BaseEntity, ErrorCode, AuditLog
├── user-service/            # OAuth2 + JWT + 관리자 승인 + 이메일 인증 (port 8081)
├── integration-service/     # Google Calendar + Gmail + Drive (port 8082)
├── ai-router-service/       # LLM 라우팅 + RAG + AI Agent (port 8083)
├── briefing-service/        # 일일 브리핑 + SSE 스트리밍 (port 8084)
├── chat-service/            # WebSocket 실시간 채팅 (port 8085)
├── gateway/                 # Spring Cloud Gateway + JWT Filter (port 8080)
├── frontend/                # React 18 + Vite + TailwindCSS 4
└── k8s/                     # Kubernetes 매니페스트 + Kustomize
```

| Module | Description |
|--------|-------------|
| common | ApiResponse, BaseEntity, ErrorCode, GlobalExceptionHandler, DateTimeUtil, AuditLog |
| user-service | Google OAuth2 로그인, JWT 발급, 관리자 승인 가입 플로우, 이메일 인증 (Redis TTL), Audit Logging |
| integration-service | Google Calendar/Gmail/Drive API 연동, Access Token 자동 갱신, 병렬 데이터 수집 |
| ai-router-service | Gemini/Ollama LLM 라우팅, AI Agent 멀티턴 대화, 컨텍스트 수집 (캘린더+이메일+드라이브+채팅) |
| briefing-service | 일일 브리핑 생성 오케스트레이션, SSE 스트리밍, 브리핑 히스토리 |
| chat-service | WebSocket STOMP 실시간 채팅, 메시지 수정/삭제/답장/리액션/검색/@멘션/Presence/파일 업로드 |
| gateway | JWT 검증 필터, CORS 설정, 서비스 라우팅 |
| frontend | 대시보드, 브리핑, 캘린더, 채팅, AI Agent, 관리자 페이지 |

---

## Key Features

### 1. 보안 아키텍처
- **관리자 승인 가입**: OAuth 로그인 → `PENDING_APPROVAL` → 관리자 승인 → 이메일 인증 (Redis TTL) → `ACTIVE`
- **JWT**: role + status claims 포함, ACTIVE가 아닌 사용자는 API 접근 제한
- **Audit Logging**: AOP 기반 감사 추적 (`@Audited`), 비동기 DB 저장
- **Google API 최소 권한**: `calendar.readonly`, `gmail.readonly`, `drive.readonly`

### 2. LLM 라우팅 전략
| Task | Model | Reason |
|------|-------|--------|
| AI Agent 채팅 | Gemini 2.5 Flash → Ollama 폴백 | 빠른 응답 + 비용 절감 |
| 일일 브리핑 | Gemini 2.5 Flash → Ollama 폴백 | 정확도 우선 |
| 텍스트 분류 | Ollama Llama 3.1 8B | Self-hosted, 비용 제로 |

### 3. 실시간 채팅 (Phase A~C 완료)
- WebSocket STOMP 기반 실시간 메시징
- 메시지 수정 (5분 제한) / 소프트 삭제 / 답장·인용
- 이모지 리액션 (6종) / @멘션 자동완성
- D&D 파일 업로드 (10MB, MIME 검증)
- 메시지 검색 / 접속 상태 Presence / 읽지 않은 메시지 배지
- 타이핑 인디케이터 / 브라우저 Push 알림

### 4. AI Agent
- 멀티턴 대화 (ConversationHistory 기반)
- 병렬 컨텍스트 수집 (캘린더 + 이메일 + 드라이브 + 채팅)
- 프롬프트 빌더를 통한 컨텍스트 주입

### 5. 브리핑 시스템
- 데이터 수집 → LLM 분석 → SSE 스트리밍 파이프라인
- 일일/회의/임시(ADHOC) 브리핑 타입 지원
- 브리핑 히스토리 관리

---

## Database Architecture

서비스별 DB 분리 전략 (MySQL 8.0):

```
workcopilot              → user-service     (users, audit_logs, notifications)
workcopilot_integration  → integration      (calendar_events)
workcopilot_ai           → ai-router        (conversation_histories, conversation_messages)
workcopilot_briefing     → briefing         (briefings)
workcopilot_chat         → chat-service     (chat_rooms, chat_participants, chat_messages, chat_files, chat_reactions)
```

---

## CI/CD & Deployment

### 자동 배포 파이프라인 (GitOps)

```
dev 브랜치 → PR → main 머지
     │
     ▼
GitHub Actions CD
     │ ① Maven 빌드 + 테스트
     │ ② Docker 이미지 빌드 (7개 서비스 + frontend)
     │ ③ GHCR 푸시 (태그: commit SHA)
     │ ④ kustomization.yaml 이미지 태그 업데이트
     │ ⑤ Auto-commit & push
     │
     ▼
ArgoCD (K3s 클러스터)
     │ ① main 브랜치 변경 자동 감지
     │ ② kubectl apply -k k8s/ 자동 실행
     │ ③ Pod 롤링 업데이트
     ▼
  배포 완료 (환경변수 변경도 자동 반영)
```

### 인프라 구성

| Component | Spec |
|-----------|------|
| Server | Windows 홈서버 (RTX GPU) |
| K8s | K3s 단일 노드 클러스터 |
| LLM | Ollama (Llama 3.1 8B + nomic-embed-text) |
| Network | Tailscale VPN + ngrok 터널링 |
| GitOps | ArgoCD (automated sync + prune + selfHeal) |
| Registry | GitHub Container Registry (GHCR) |

---

## Claude Code 활용 (AI Native 개발)

이 프로젝트는 Claude Code를 전면적으로 활용하여 개발되었습니다.

### CLAUDE.md — 프로젝트 컨벤션
- 패키지 구조, 에러 처리 패턴, API 응답 형식, 테스트 네이밍 등 전체 코딩 컨벤션 정의
- 모듈 구조, 프로파일 설정, LLM 라우팅 전략, 배포 규칙까지 포함
- Claude Code가 항상 프로젝트 맥락을 이해한 상태에서 코드 생성

### Slash Commands (6종)
| Command | Description |
|---------|-------------|
| `/daily` | dev-guide 기반 현재 진행 상태 + 오늘 할 일 분석 |
| `/guide N` | Day별 구현 가이드 로드 및 요약 |
| `/test-all` | 전체 모듈 테스트 실행 + 결과 분석 |
| `/pr` | git diff 기반 PR 내용 자동 생성 |
| `/deploy [service]` | 서비스별 빌드 → Docker → K3s 배포 |
| `/infra-check` | K3s 클러스터 + Ollama 상태 확인 |

### Custom Skills (5종)
| Skill | Description |
|-------|-------------|
| `implement-feature` | Entity → Repository → DTO → Service → Controller → Tests 순서 강제 |
| `write-tests` | MockitoExtension + BDD 패턴 + 한글 테스트명 컨벤션 |
| `create-api` | REST API 설계 패턴 + ApiResponse 래핑 + 입력 검증 |
| `code-review` | 구조/보안/성능/에러처리/코드품질 5개 영역 체크리스트 |
| `create-k8s` | K8s 매니페스트 템플릿 + 포트 매핑 + 헬스 프로브 |

### Custom Agents (3종)
| Agent | Description |
|-------|-------------|
| `security-reviewer` | JWT 관리, OAuth2 토큰 저장, API 인증 커버리지, CORS, SQL Injection 분석 |
| `performance-analyzer` | N+1 쿼리, 페이지네이션, 인덱스, Redis 캐싱, 외부 API 타임아웃 분석 |
| `api-documenter` | Controller 분석 → API 문서 자동 생성 (Method, Path, DTO, 에러코드) |

### 서브에이전트 병렬 처리
- Git Worktree를 활용한 다중 서브에이전트 동시 작업
- 7개 마이크로서비스를 병렬로 개발 (integration-service, gateway, frontend 등 동시 진행)
- Task 도구로 탐색/분석/구현을 분리하여 효율적 컨텍스트 관리

### Persistent Memory
- 세션 간 프로젝트 컨텍스트 유지 (MEMORY.md)
- 채팅 Phase A~C 진행 상황, 버그 수정 이력, 빌드 명령어 등 자동 기록
- 이전 세션의 결정 사항을 다음 세션에서 즉시 참조

---

## Quick Start

### Docker Compose (로컬 개발)
```bash
# 전체 서비스 실행
docker compose up -d --build

# 프론트엔드: http://localhost:5173
# Gateway: http://localhost:8080
```

### 개별 서비스 실행
```bash
# 백엔드 빌드
mvn clean install -DskipTests

# 서비스별 실행 (local 프로파일)
cd user-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd chat-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
# ...

# 프론트엔드
cd frontend && npm install && npm run dev
```

### 테스트
```bash
mvn test    # 전체 모듈 테스트
mvn test -pl :chat-service -am    # 특정 모듈만
```

---

## Development Phases

| Phase | Period | Description |
|-------|--------|-------------|
| 1. Foundation | Day 1-6 | 개발 환경 + K3s + Ollama + Claude Code 셋업 |
| 2. Core Services | Day 7-18 | user/integration/ai-router/briefing/gateway/frontend |
| 3. Stabilization | Day 19-25 | 프롬프트 튜닝, Dockerfile, K8s, CI/CD, OpenAPI |
| 4. Infra Migration | - | H2/PostgreSQL → MySQL 8.0, Gemini LLM 전환 |
| 5. Chat + AI Agent | - | WebSocket 채팅, 멀티턴 AI Agent |
| 6. Chat Enhancement | - | Phase A~C (수정/리액션/답장/검색/멘션/Presence) |
| 7. GitOps | - | ArgoCD 자동 배포 파이프라인 구축 |
