# 배포 체크리스트

## 1. 코드만 변경 (기존 기능 수정/추가)

기존 서비스의 코드만 수정한 경우. **push만 하면 끝.**

```bash
git add <files> && git commit -m "feat: ..." && git push origin main
```

- [ ] 빌드 확인: `mvn compile -pl <module> -q`
- [ ] 테스트 통과: `mvn test -pl <module>`
- [ ] push 후 CD 성공 확인: `gh run list --limit 1`

---

## 2. 새 환경변수 추가

application.yml에 `${NEW_VAR}` 형태의 환경변수를 추가한 경우.

- [ ] `k8s/<service>/deployment.yaml`에 env 항목 추가
- [ ] 민감한 값이면 `k8s/secrets.yaml`에 Secret 추가 후 SSH apply:
  ```bash
  ssh homeserver 'kubectl apply -f - -n workcopilot' < k8s/secrets.yaml
  ```
- [ ] 공개 가능한 값이면 deployment.yaml에 직접 `value:` 작성

> **주의**: K8s는 서비스명으로 환경변수를 자동 생성함 (예: `redis` 서비스 → `REDIS_PORT=tcp://...`)
> Spring의 `${SERVICE_PORT}` 같은 변수명이 충돌할 수 있으므로 deployment.yaml에 명시적으로 설정

---

## 3. 새 서비스(Pod) 추가

새로운 마이크로서비스를 추가하는 경우.

### 필요한 파일 생성

```
k8s/<new-service>/
  ├── deployment.yaml
  ├── service.yaml
  └── pvc.yaml          # H2 파일 DB 사용 시
```

### deployment.yaml 필수 항목

- [ ] `namespace: workcopilot`
- [ ] `image: ghcr.io/parkhyeonbeom/workcopilot-<service>:latest`
- [ ] `SPRING_PROFILES_ACTIVE: k8s`
- [ ] 필요한 Secret 참조 (jwt-secret, google-oauth 등)
- [ ] H2 파일 DB 사용 시:
  - [ ] `strategy.type: Recreate` (파일 잠금 방지)
  - [ ] `volumeMounts` + `volumes` (PVC 마운트)
- [ ] `resources` (requests/limits)
- [ ] `readinessProbe` + `livenessProbe`

### kustomization.yaml 등록

- [ ] `resources` 섹션에 추가:
  ```yaml
  - <new-service>/deployment.yaml
  - <new-service>/service.yaml
  - <new-service>/pvc.yaml  # 있으면
  ```
- [ ] `images` 섹션에 추가:
  ```yaml
  - name: ghcr.io/parkhyeonbeom/workcopilot-<new-service>
    newName: ghcr.io/parkhyeonbeom/workcopilot-<new-service>
    newTag: latest
  ```

### CI/CD 파이프라인

- [ ] `.github/workflows/cd.yml`에 새 서비스 빌드 단계 추가
- [ ] Dockerfile 작성 (기존 서비스 참고)

### Gateway 라우팅

- [ ] `gateway/.../application.yml`에 라우트 추가:
  ```yaml
  - id: <new-service>
    uri: http://<new-service>:<port>
    predicates:
      - Path=/api/<prefix>/**
  ```

---

## 4. 새 인프라 의존성 추가 (Redis, Kafka 등)

서비스 코드에서 새로운 외부 시스템을 사용하는 경우.

- [ ] `k8s/<infra>/deployment.yaml` + `service.yaml` 생성
- [ ] `kustomization.yaml`에 리소스 등록
- [ ] 사용하는 서비스의 deployment.yaml에 연결 환경변수 추가:
  ```yaml
  - name: <INFRA>_HOST
    value: "<k8s-service-name>"
  - name: <INFRA>_PORT    # K8s 자동 생성 env와 충돌 방지
    value: "<port>"
  ```
- [ ] application.yml의 k8s 프로필에 설정 추가

---

## 5. application.yml k8s 프로필 점검

새 설정을 추가할 때 **local 프로필에만 넣고 k8s 프로필에 빠뜨리는 실수** 방지.

- [ ] local 프로필에 추가한 설정이 k8s 프로필에도 있는지 확인
- [ ] DB URL: `jdbc:h2:file:/data/<db>` (절대경로, Pod 내부)
- [ ] `ddl-auto: update` (create-drop 절대 금지)
- [ ] 프론트엔드 URL: `https://nonnaval-tragicomical-eliana.ngrok-free.dev`
- [ ] OAuth redirect URI: ngrok 도메인 사용

---

## 6. 배포 후 검증

```bash
# Pod 상태 확인
ssh homeserver 'kubectl get pods -n workcopilot'

# 특정 서비스 로그 확인
ssh homeserver 'kubectl logs deploy/<service> -n workcopilot --tail=30'

# 에러 로그만 필터
ssh homeserver 'kubectl logs deploy/<service> -n workcopilot --tail=100 | grep -i error'

# Pod 재시작 횟수 확인 (0이어야 정상)
ssh homeserver 'kubectl get pods -n workcopilot -o wide'
```

---

## 빠른 참조: 현재 인프라 구성

| 항목 | 값 |
|------|---|
| 도메인 | `nonnaval-tragicomical-eliana.ngrok-free.dev` |
| K8s | K3s 단일 노드 (Windows 홈서버) |
| SSH | `ssh homeserver` |
| kubectl | `kh` (= KUBECONFIG=~/.kube/config-home kubectl) |
| CD | GitHub Actions → GHCR → ArgoCD 자동 sync |
| DB | H2 파일 기반 + PVC (user-service, integration-service) |
| 캐시 | Redis 7 (workcopilot namespace) |
| Secrets | jwt-secret, google-oauth, mail-secret, openai-secret |
