package com.workcopilot.chat.websocket;

import com.workcopilot.chat.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        StompPrincipal principal = (StompPrincipal) accessor.getUser();
        if (principal == null) return;

        Long userId = principal.getUserId();
        String sessionId = accessor.getSessionId();

        boolean wasOffline = !presenceService.isOnline(userId);
        presenceService.userConnected(userId, sessionId);

        if (wasOffline) {
            broadcastPresence(userId, true);
        }

        log.info("WebSocket 세션 연결: userId={}, sessionId={}", userId, sessionId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        StompPrincipal principal = (StompPrincipal) accessor.getUser();
        if (principal == null) return;

        Long userId = principal.getUserId();
        String sessionId = accessor.getSessionId();

        presenceService.userDisconnected(userId, sessionId);

        if (!presenceService.isOnline(userId)) {
            broadcastPresence(userId, false);
        }

        log.info("WebSocket 세션 해제: userId={}, sessionId={}", userId, sessionId);
    }

    private void broadcastPresence(Long userId, boolean online) {
        Map<String, Object> event = Map.of(
                "type", "PRESENCE",
                "userId", userId,
                "online", online
        );
        messagingTemplate.convertAndSend("/topic/presence", event);
    }
}
