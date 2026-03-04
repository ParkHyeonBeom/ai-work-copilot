# AI Work Copilot - 프로젝트 규칙

## 프로젝트 개요
사내용 업무 분석 자동화 솔루션.
Google Calendar + Gmail + Drive 데이터를 Self-hosted LLM으로 분석하여 일일 업무 브리핑을 자동 생성.
관리자 승인 기반 접근 제어 + Audit Logging으로 기업 보안 요건 충족.

## 보안 아키텍처
- **Self-hosted LLM**: Ollama (Llama 3.1 8B) 사내 서버 운영, 외부 API 의존 최소화
- **관리자 승인 가입**: OAuth 로그인 → PENDING_APPROVAL → 관리자 승인 → 이메일 인증(Redis TTL) → ACTIVE
- **Audit Logging**: AOP 기반 감사 추적 (@Audited), 비동기 DB 저장
- **Google API**: calendar.readonly, gmail.readonly, drive.readonly (최소 권한)
- **JWT**: role + status claims 포함, ACTIVE가 아닌 사용자는 API 접근 제한

## 기술 스택
- Java 21, Spring Boot 3.3.6, Spring AI 1.0.0-M4, Maven 멀티모듈
- MySQL 8.0, Redis 7, Kafka (KRaft), Milvus 2.4
- React 18, Vite, TailwindCSS 4
- K3s (Windows 홈서버), Ollama (Llama 3.1 8B + nomic-embed-text)
- Docker Compose (로컬 개발 환경)

## 모듈 구조
```
common/               → 공통 (ApiResponse, BaseEntity, ErrorCode, AuditLog)
user-service/          → OAuth2 + JWT + 관리자 승인 + 이메일 인증 (포트 8081)
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
- `local`: MySQL 8.0 (Docker Compose) + Redis (Docker Compose)
- `k8s`: MySQL 8.0 (K8s StatefulSet) + Redis (K8s Pod) + K3s 클러스터 환경
- `prod`: MySQL 8.0 + Redis + Kafka + Milvus

## LLM 라우팅
| 작업 | 모델 | 이유 |
|------|------|------|
| 텍스트 분류/키워드 | Ollama Llama 3.1 8B | Self-hosted, 비용 절감 |
| 종합 브리핑 | Claude Sonnet → Ollama 폴백 | 정확도 우선 |
| 긴 문서 요약 | Claude Sonnet → Ollama 폴백 | 긴 컨텍스트 |

## 인프라 현황 + 제한사항
- Windows 홈서버 1대: K3s 단일 노드, Ollama GPU (RTX)
- 네트워크: Tailscale VPN + ngrok 터널링
- 제한: HA 미지원 (단일 노드), GPU 메모리 제한으로 8B 모델만 운용
- 서버 접속: `ssh homeserver` (= `ssh -p 2222 gusqja@100.95.227.98`)
- K3s: `kh get nodes` (= `KUBECONFIG=~/.kube/config-home kubectl`)

## API Swagger
- Swagger UI/ OpenAPI를 활용한 API 문서화지금은 

## 개발 가이드
상세 구현 가이드: `docs/dev-guide.md` → `/guide N` 커맨드로 Day별 참조

## 배포 규칙 (필수)
**배포 관련 작업 시 반드시 `docs/deploy-checklist.md`를 참조할 것.**
- 코드만 변경 → `git push`만으로 CD 자동 배포
- 새 환경변수/Secret 추가 → deployment.yaml + secrets.yaml 동시 수정
- 새 서비스(Pod) 추가 → deployment + service + kustomization + CD + Gateway 라우트
- 새 인프라 추가 → K8s 매니페스트 + 연결 env (포트 충돌 주의)
- **local 프로필 설정 추가 시 k8s 프로필에도 반드시 동일 추가**

## Google OAuth2
- Google Cloud Console에서 OAuth2 Client ID/Secret 발급 완료
- 환경변수: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (K8s Secret으로 관리)
- 리디렉션 URI: `http://localhost:30081/login/oauth2/code/google`

## 프로젝트 진행 
- 수정사항이 많은 경우, 적절히 서브에이전트를 활용하여 병렬로 처리할것
- 이때 이미 작성된 커맨드, 스킬을 적절히 활용하여 수정의 퀄리티를 높힐것
- 테스트를 철저히하여 완성도를 높히는것에 초점을 둘것
- 잔여작업은 항상 dev-guide 문서를 통해 최신화 할것
- 커밋/푸쉬시에 dev-guide 문서를 최신화 할것 