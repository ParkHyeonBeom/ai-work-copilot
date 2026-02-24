# Claude Code 설정 가이드

## 1. 파일 배치

이 zip의 파일들을 프로젝트 루트에 덮어씁니다:

```bash
cd ~/Downloads/ai-work-copilot

# 기존 CLAUDE.md 덮어쓰기 + 새 파일 추가
cp -r [zip 풀린 경로]/.claude/ .
cp -r [zip 풀린 경로]/docs/ .
```

최종 구조:
```
ai-work-copilot/
├── .claude/
│   ├── CLAUDE.md                    ← 프로젝트 규칙 (매번 자동 로드)
│   ├── commands/
│   │   ├── daily.md                 ← /daily
│   │   ├── pr.md                    ← /pr
│   │   ├── test-all.md              ← /test-all
│   │   ├── deploy.md                ← /deploy
│   │   ├── infra-check.md           ← /infra-check
│   │   └── guide.md                 ← /guide N
│   ├── skills/
│   │   ├── implement-feature.md     ← 기능 구현 패턴
│   │   ├── write-tests.md           ← 테스트 작성 패턴
│   │   ├── create-api.md            ← REST API 패턴
│   │   ├── code-review.md           ← 코드 리뷰 체크리스트
│   │   └── create-k8s.md            ← K8s 매니페스트 패턴
│   └── agents/
│       ├── security-reviewer.md     ← 보안 리뷰
│       ├── performance-analyzer.md  ← 성능 분석
│       └── api-documenter.md        ← API 문서 생성
└── docs/
    └── dev-guide.md                 ← 25일 전체 개발 가이드
```

## 2. IntelliJ Claude Code 플러그인 설치

1. IntelliJ → Settings → Plugins → Marketplace
2. "Claude" 검색 → "Claude Code" 설치
3. IntelliJ 재시작
4. 우측 패널에 Claude Code 탭 확인

## 3. API 키 설정

Claude Code 플러그인 설정에서 Anthropic API 키 입력.
또는 환경변수로:
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

## 4. 사용법

### 슬래시 커맨드
IntelliJ Claude Code 패널에서 입력:

| 커맨드 | 용도 |
|--------|------|
| `/daily` | 오늘 상태 확인 + 다음 작업 파악 |
| `/guide 8` | Day 8 가이드 로드 |
| `/test-all` | 전체 테스트 실행 + 결과 분석 |
| `/pr` | PR 내용 자동 생성 |
| `/deploy user-service` | K3s 배포 |
| `/infra-check` | 서버 인프라 상태 확인 |

### 매일 루틴
```
1. /daily              → 현재 상태 확인
2. /guide N            → 해당 Day 가이드 로드
3. "진행해줘"           → 구현 시작
4. /test-all           → 테스트
5. /pr                 → PR 준비
6. git add . && git commit && git push
```

### 스킬 활용 (자동)
Claude Code가 코드를 작성할 때 `.claude/skills/`의 패턴을 자동으로 참고합니다:
- 기능 구현 요청 → implement-feature.md 패턴 적용
- 테스트 작성 요청 → write-tests.md 패턴 적용
- API 생성 요청 → create-api.md 패턴 적용

### 에이전트 활용 (수동 호출)
```
"security-reviewer 에이전트로 user-service 보안 점검해줘"
"performance-analyzer로 BriefingService 성능 분석해줘"
"api-documenter로 user-service API 문서 생성해줘"
```

## 5. 커밋 후 push

설정 파일들을 커밋합니다:
```bash
cd ~/Downloads/ai-work-copilot
git add .
git commit -m "chore: Claude Code 스킬/에이전트/커맨드 + dev-guide 추가"
git push
```
