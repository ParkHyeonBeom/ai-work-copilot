# K8s 매니페스트 생성 스킬

K3s에 배포할 Kubernetes 매니페스트를 생성한다.

## 파일 구조
```
infra/k8s/{서비스명}/
├── deployment.yaml
├── service.yaml
├── configmap.yaml
└── hpa.yaml (선택)
```

## Deployment 템플릿
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {서비스명}
  namespace: workcopilot
  labels:
    app: {서비스명}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: {서비스명}
  template:
    metadata:
      labels:
        app: {서비스명}
    spec:
      containers:
        - name: {서비스명}
          image: workcopilot/{서비스명}:latest
          ports:
            - containerPort: {포트}
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
          envFrom:
            - configMapRef:
                name: {서비스명}-config
            - secretRef:
                name: workcopilot-secrets
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: {포트}
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: {포트}
            initialDelaySeconds: 60
            periodSeconds: 30
```

## 포트 매핑
| 서비스 | 포트 |
|--------|------|
| gateway | 8080 |
| user-service | 8081 |
| integration-service | 8082 |
| ai-router-service | 8083 |
| briefing-service | 8084 |
