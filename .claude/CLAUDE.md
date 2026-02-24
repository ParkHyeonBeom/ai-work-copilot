# AI Work Copilot - 프로젝트 규칙

## 프로젝트 개요
Google Calendar + Gmail + Drive를 분석하여 AI 업무 브리핑을 제공하는 서비스.
Salesforce 개발자의 백엔드 이직 포트폴리오.

## 기술 스택
- Java 21, Spring Boot 3.3.6, Spring AI 1.0.0-M4, Maven 멀티모듈
- PostgreSQL 16, Redis 7, Kafka (KRaft), Milvus 2.4
- React 18, Vite, TailwindCSS 4
- K3s (Windows 홈서버), Ollama (Llama 3.1 8B + nomic-embed-text)

## 모듈 구조
```
common/               → 공통 (ApiResponse, BaseEntity, ErrorCode, DomainEvent)
user-service/          → OAuth2 + JWT + 회원 + 온보딩 (포트 8081)
integration-service/   → Google Calendar + Gmail + Drive (포트 8082)
ai-router-service/     → Spring AI: LLM 라우팅 + RAG (포트 8083)
briefing-service/      → 일일 브리핑, SSE 스트리밍 (포트 8084)
gateway/              → Spring Cloud Gateway (포트 8080)
frontend/             → React 18 + Vite (포트 5173)
```

## 코딩 컨벤션
- 패키지: `com.workcopilot.{모듈명}`
- DTO: Java record 사용 권장
- 에러: `throw new BusinessException(ErrorCode.XXX)`
- API 응답: `ApiResponse.ok(data)` 또는 `ApiResponse.error(msg, code)`
- 엔티티: `BaseEntity` 상속 (id, createdAt, updatedAt 자동)
- 의존성 주입: 생성자 주입 (`@RequiredArgsConstructor`)
- 로깅: `@Slf4j` + `log.info/warn/error`

## 테스트 컨벤션
- 단위 테스트 필수: `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks`
- `@SpringBootTest`는 통합 테스트에만 사용
- 테스트 메서드명: `메서드명_조건_기대결과` (예: `findByEmail_존재하는이메일_유저반환`)

## 프로파일
- `local`: H2 인메모리 + Redis/Kafka 없이 동작
- `prod`: PostgreSQL + Redis + Kafka + Milvus

## LLM 라우팅
| 작업 | 모델 | 이유 |
|------|------|------|
| 텍스트 분류/키워드 | Ollama Llama 3.1 8B | 비용 절감 |
| 종합 브리핑 | GPT-4o (Function Calling) | 정확도 |
| 긴 문서 요약 | Claude Sonnet | 긴 컨텍스트 |

## 서버 접속
- Windows 서버: `ssh homeserver` (= `ssh -p 2222 gusqja@100.95.227.98`)
- K3s: `kh get nodes` (= `KUBECONFIG=~/.kube/config-home kubectl`)

## 개발 가이드
상세 구현 가이드: `docs/dev-guide.md` → `/guide N` 커맨드로 Day별 참조
