# AI Work Copilot

Google Calendar + Gmail + Drive 데이터를 분석하여 AI 업무 브리핑을 제공하는 마이크로서비스 플랫폼.

## Architecture

```
[React Frontend]
       |
  [API Gateway :8080]
    /      |       \        \
[User]  [Integration] [AI Router] [Briefing]
:8081     :8082        :8083       :8084
  |         |            |           |
[Google OAuth2]  [Google APIs]  [GPT-4o/Ollama]  [SSE Stream]
                                     |
                              [Vector Store]
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.6, Spring AI 1.0.0-M4 |
| API Gateway | Spring Cloud Gateway |
| Database | PostgreSQL 16, H2 (local) |
| Cache | Redis 7 |
| Message Queue | Apache Kafka (KRaft) |
| Vector DB | Milvus 2.4 |
| LLM | OpenAI GPT-4o, Ollama (Llama 3.1 8B) |
| Frontend | React 18, Vite, TailwindCSS 4 |
| Infra | K3s, Docker |
| CI/CD | GitHub Actions |

## Modules

```
ai-work-copilot/
├── common/                  # Shared: ApiResponse, BaseEntity, ErrorCode
├── user-service/            # OAuth2 + JWT + Onboarding (port 8081)
├── integration-service/     # Google Calendar + Gmail + Drive (port 8082)
├── ai-router-service/       # LLM Routing + RAG + Embeddings (port 8083)
├── briefing-service/        # Daily Briefing + SSE Streaming (port 8084)
├── gateway/                 # Spring Cloud Gateway + JWT Filter (port 8080)
├── frontend/                # React Dashboard + Onboarding UI
└── k8s/                     # Kubernetes manifests
```

| Module | Source Files | Tests | Description |
|--------|:-----------:|:-----:|-------------|
| common | 12 | 17 | ApiResponse, BaseEntity, ErrorCode, GlobalExceptionHandler, DateTimeUtil |
| user-service | 22 | 13 | Google OAuth2 login, JWT token issuance, user onboarding |
| integration-service | 22 | 17 | Google Calendar/Gmail/Drive API integration, token storage |
| ai-router-service | 28 | 18 | LLM routing (GPT-4o/Ollama), prompt builders, vector store |
| briefing-service | 24 | 14 | Daily briefing generation, SSE streaming, history |
| gateway | 8 | 19 | JWT validation filter, rate limiting, CORS, routing |
| **Total** | **116** | **98** | |

## Key Features

### LLM Routing Strategy
| Task | Model | Reason |
|------|-------|--------|
| Text Classification / Keywords | Ollama (Llama 3.1 8B) | Cost reduction |
| Daily Briefing | GPT-4o (Function Calling) | Accuracy |
| Document Summarization | GPT-4o | Long context support |

### Briefing Flow
1. User requests daily briefing
2. `briefing-service` calls `integration-service` to collect today's calendar events, emails, and drive files
3. `briefing-service` calls `ai-router-service` with collected data
4. `ai-router-service` routes to GPT-4o, builds structured prompt, returns briefing
5. Briefing is stored and streamed to frontend via SSE

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- Node.js 18+

### Backend
```bash
# Build all modules
mvn clean install

# Run each service (local profile uses H2 in-memory DB)
cd user-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd integration-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd ai-router-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd briefing-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd gateway && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Frontend
```bash
cd frontend
npm install
npm run dev
# Open http://localhost:5173
```

### Run Tests
```bash
mvn test
# 98 tests across 6 modules
```

## API Endpoints

### User Service (via Gateway :8080)
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/users/auth/google | Google OAuth2 login |
| GET | /api/users/auth/google/callback | OAuth2 callback |
| GET | /api/users/me | Get current user |
| POST | /api/users/onboarding | Complete onboarding |

### Integration Service
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/integrations/calendar/today | Today's events |
| GET | /api/integrations/calendar/upcoming | Upcoming events |
| GET | /api/integrations/gmail/recent | Recent emails |
| GET | /api/integrations/gmail/important | Important emails |
| GET | /api/integrations/drive/recent | Recent files |
| GET | /api/integrations/data/collect | Collect all data |

### AI Router Service
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/ai/briefing | Generate AI briefing |
| POST | /api/ai/classify | Classify content |
| POST | /api/ai/summarize | Summarize document |

### Briefing Service
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/briefings/daily | Generate daily briefing |
| GET | /api/briefings/today | Get today's briefing |
| GET | /api/briefings/{id} | Get specific briefing |
| GET | /api/briefings/{id}/stream | SSE streaming |
| GET | /api/briefings/history | Briefing history |

## K3s Deployment

```bash
# Build JARs
mvn clean package -DskipTests

# Build Docker images
docker build -t workcopilot/user-service:latest -f user-service/Dockerfile user-service/target/
docker build -t workcopilot/integration-service:latest -f integration-service/Dockerfile integration-service/target/
docker build -t workcopilot/ai-router-service:latest -f ai-router-service/Dockerfile ai-router-service/target/
docker build -t workcopilot/briefing-service:latest -f briefing-service/Dockerfile briefing-service/target/
docker build -t workcopilot/gateway:latest -f gateway/Dockerfile gateway/target/

# Deploy to K3s
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/user-service/
kubectl apply -f k8s/integration-service/
kubectl apply -f k8s/ai-router-service/
kubectl apply -f k8s/briefing-service/
kubectl apply -f k8s/gateway/
```

## Project Structure Highlights

- **JWT Shared Secret**: All services validate JWT tokens using the same shared secret
- **Token Forwarding**: `briefing-service` forwards JWT to downstream services via RestTemplate interceptor
- **Graceful Degradation**: `ai-router-service` returns mock responses when LLM APIs are unavailable
- **InMemory Vector Store**: Local profile uses ConcurrentHashMap-based vector store; production uses Milvus
- **SSE Streaming**: Briefing content is chunked and streamed in real-time to the frontend
