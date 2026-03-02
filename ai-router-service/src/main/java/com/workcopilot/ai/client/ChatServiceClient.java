package com.workcopilot.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class ChatServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String chatServiceUrl;

    public ChatServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${chat-service.url}") String chatServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.chatServiceUrl = chatServiceUrl;
    }

    /**
     * chat-service에서 사용자의 채팅방 목록을 조회하여
     * 채팅방 이름과 마지막 메시지 정보를 텍스트로 반환한다.
     */
    public String getRecentMessages(Long userId, String token) {
        try {
            String url = chatServiceUrl + "/api/chat/rooms";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (token != null) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            return formatChatRoomsResponse(response.getBody());
        } catch (Exception e) {
            log.warn("채팅 서비스 호출 실패: userId={}, error={}", userId, e.getMessage());
            return "";
        }
    }

    private String formatChatRoomsResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "";
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (!data.isArray() || data.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            for (JsonNode room : data) {
                String roomName = room.has("name") ? room.get("name").asText() : "이름 없음";
                String roomType = room.has("type") ? room.get("type").asText() : "";
                int memberCount = room.has("memberCount") ? room.get("memberCount").asInt() : 0;
                String lastMessage = room.has("lastMessage") ? room.get("lastMessage").asText() : "";

                sb.append("- [").append(roomType).append("] ").append(roomName);
                if (memberCount > 0) sb.append(" (").append(memberCount).append("명)");
                if (!lastMessage.isEmpty() && !"null".equals(lastMessage)) {
                    sb.append("\n  최근: ").append(lastMessage);
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("채팅방 응답 파싱 실패: {}", e.getMessage());
            return responseBody;
        }
    }
}
