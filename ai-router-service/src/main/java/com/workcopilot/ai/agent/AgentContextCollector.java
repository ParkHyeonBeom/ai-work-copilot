package com.workcopilot.ai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workcopilot.ai.client.ChatServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class AgentContextCollector {

    private final String integrationServiceUrl;
    private final RestTemplate restTemplate;
    private final ChatServiceClient chatServiceClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    public AgentContextCollector(
            @Value("${integration-service.url}") String integrationServiceUrl,
            RestTemplate restTemplate,
            ChatServiceClient chatServiceClient,
            ObjectMapper objectMapper
    ) {
        this.integrationServiceUrl = integrationServiceUrl;
        this.restTemplate = restTemplate;
        this.chatServiceClient = chatServiceClient;
        this.objectMapper = objectMapper;
        this.executor = Executors.newFixedThreadPool(4);
    }

    /**
     * 4개 소스(캘린더, 이메일, 드라이브, 채팅)에서 병렬로 컨텍스트를 수집한다.
     */
    public AgentContext collectContext(Long userId, String token) {
        log.info("에이전트 컨텍스트 수집 시작: userId={}", userId);

        CompletableFuture<String> calendarFuture = CompletableFuture.supplyAsync(
                () -> collectCalendarEvents(token), executor);
        CompletableFuture<String> emailFuture = CompletableFuture.supplyAsync(
                () -> collectRecentEmails(token), executor);
        CompletableFuture<String> driveFuture = CompletableFuture.supplyAsync(
                () -> collectRecentFiles(token), executor);
        CompletableFuture<String> chatFuture = CompletableFuture.supplyAsync(
                () -> chatServiceClient.getRecentMessages(userId, token), executor);

        CompletableFuture.allOf(calendarFuture, emailFuture, driveFuture, chatFuture).join();

        String calendarEvents = getResultSafely(calendarFuture, "calendar");
        String recentEmails = getResultSafely(emailFuture, "email");
        String recentFiles = getResultSafely(driveFuture, "drive");
        String recentChats = getResultSafely(chatFuture, "chat");

        log.info("에이전트 컨텍스트 수집 완료: userId={}, calendar={}, email={}, drive={}, chat={}",
                userId,
                !calendarEvents.isEmpty(),
                !recentEmails.isEmpty(),
                !recentFiles.isEmpty(),
                !recentChats.isEmpty());

        return new AgentContext(calendarEvents, recentChats, recentEmails, recentFiles);
    }

    private String collectCalendarEvents(String token) {
        try {
            String url = integrationServiceUrl + "/api/integrations/calendar/events?days=2";
            HttpEntity<Void> entity = createAuthEntity(token);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            return formatCalendarResponse(response.getBody());
        } catch (Exception e) {
            log.warn("캘린더 이벤트 수집 실패: {}", e.getMessage());
            return "";
        }
    }

    private String collectRecentEmails(String token) {
        try {
            String url = integrationServiceUrl + "/api/integrations/gmail/messages/recent?max=10";
            HttpEntity<Void> entity = createAuthEntity(token);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            return formatEmailResponse(response.getBody());
        } catch (Exception e) {
            log.warn("이메일 수집 실패: {}", e.getMessage());
            return "";
        }
    }

    private String collectRecentFiles(String token) {
        try {
            String url = integrationServiceUrl + "/api/integrations/drive/files?max=5";
            HttpEntity<Void> entity = createAuthEntity(token);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            return formatDriveResponse(response.getBody());
        } catch (Exception e) {
            log.warn("드라이브 파일 수집 실패: {}", e.getMessage());
            return "";
        }
    }

    private HttpEntity<Void> createAuthEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(headers);
    }

    private String formatCalendarResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "";
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (!data.isArray() || data.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            for (JsonNode event : data) {
                String summary = event.has("summary") ? event.get("summary").asText() : "제목 없음";
                String start = event.has("startTime") ? event.get("startTime").asText() : "";
                String end = event.has("endTime") ? event.get("endTime").asText() : "";
                String location = event.has("location") ? event.get("location").asText() : "";

                sb.append("- ").append(summary);
                if (!start.isEmpty()) sb.append(" (").append(start);
                if (!end.isEmpty()) sb.append(" ~ ").append(end);
                if (!start.isEmpty()) sb.append(")");
                if (!location.isEmpty()) sb.append(" [장소: ").append(location).append("]");
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("캘린더 응답 파싱 실패: {}", e.getMessage());
            return responseBody;
        }
    }

    private String formatEmailResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "";
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (!data.isArray() || data.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            for (JsonNode email : data) {
                String subject = email.has("subject") ? email.get("subject").asText() : "제목 없음";
                String from = email.has("from") ? email.get("from").asText() : "";
                String snippet = email.has("snippet") ? email.get("snippet").asText() : "";
                boolean isRead = email.has("isRead") && email.get("isRead").asBoolean();

                sb.append("- ").append(isRead ? "[읽음] " : "[안읽음] ");
                sb.append(subject);
                if (!from.isEmpty()) sb.append(" (보낸이: ").append(from).append(")");
                if (!snippet.isEmpty()) sb.append("\n  ").append(snippet);
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("이메일 응답 파싱 실패: {}", e.getMessage());
            return responseBody;
        }
    }

    private String formatDriveResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "";
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (!data.isArray() || data.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            for (JsonNode file : data) {
                String name = file.has("name") ? file.get("name").asText() : "이름 없음";
                String mimeType = file.has("mimeType") ? file.get("mimeType").asText() : "";
                String modifiedTime = file.has("modifiedTime") ? file.get("modifiedTime").asText() : "";

                sb.append("- ").append(name);
                if (!mimeType.isEmpty()) sb.append(" [").append(mimeType).append("]");
                if (!modifiedTime.isEmpty()) sb.append(" (수정: ").append(modifiedTime).append(")");
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("드라이브 응답 파싱 실패: {}", e.getMessage());
            return responseBody;
        }
    }

    private String getResultSafely(CompletableFuture<String> future, String source) {
        try {
            return future.get();
        } catch (Exception e) {
            log.warn("{} 컨텍스트 수집 실패: {}", source, e.getMessage());
            return "";
        }
    }

    /**
     * 수집된 에이전트 컨텍스트 데이터
     */
    public record AgentContext(
            String calendarEvents,
            String recentChats,
            String recentEmails,
            String recentFiles
    ) {
        public boolean isEmpty() {
            return (calendarEvents == null || calendarEvents.isBlank())
                    && (recentChats == null || recentChats.isBlank())
                    && (recentEmails == null || recentEmails.isBlank())
                    && (recentFiles == null || recentFiles.isBlank());
        }
    }
}
