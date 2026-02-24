지정한 서비스를 K3s에 배포해줘.

사용법: /deploy [서비스명]  (예: /deploy user-service)

1. 서비스 빌드: `mvn -pl {서비스명} -am clean package -DskipTests`
2. Docker 이미지 빌드: `docker build -t workcopilot/{서비스명}:latest {서비스명}/`
3. K3s에 배포:
   ```
   kh apply -f infra/k8s/{서비스명}/
   kh rollout restart deployment/{서비스명} -n workcopilot
   ```
4. 배포 상태 확인:
   ```
   kh rollout status deployment/{서비스명} -n workcopilot --timeout=60s
   kh get pods -n workcopilot -l app={서비스명}
   ```
5. 로그 확인: `kh logs -n workcopilot -l app={서비스명} --tail=20`

결과 보고:
```
🚀 배포 결과: {서비스명}
상태: ✅ 성공 / ❌ 실패
Pod: {pod명} (Running/Error)
```
