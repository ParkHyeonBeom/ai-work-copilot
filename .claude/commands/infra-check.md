인프라 상태를 확인해줘.

1. K3s 클러스터:
   ```
   kh get nodes
   kh get pods -n workcopilot
   ```

2. 서비스 상태:
   ```
   kh get svc -n workcopilot
   ```

3. 리소스 사용량:
   ```
   kh top nodes
   kh top pods -n workcopilot
   ```

4. Ollama (LLM):
   ```
   curl -s http://100.95.227.98:11434/api/tags | jq '.models[].name'
   ```

결과 보고:
```
🖥️ 인프라 상태
━━━━━━━━━━━━━━━━━━━━
K3s 노드: ✅/❌
PostgreSQL: ✅/❌
Redis: ✅/❌
Kafka: ✅/❌
Milvus: ✅/❌
Ollama: ✅/❌ (모델: llama3.1:8b, nomic-embed-text)

[문제가 있으면 해결 방법 제시]
```
