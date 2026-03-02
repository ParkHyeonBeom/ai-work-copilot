package com.workcopilot.chat.websocket;

import com.workcopilot.chat.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");

            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                String token = extractToken(authHeader);

                if (token != null && jwtProvider.validateToken(token)) {
                    Long userId = jwtProvider.getUserIdFromToken(token);
                    String name = jwtProvider.getNameFromToken(token);

                    StompPrincipal principal = new StompPrincipal(userId, name != null ? name : "User");
                    accessor.setUser(principal);

                    log.info("WebSocket 연결 인증 성공: userId={}, name={}", userId, name);
                } else {
                    log.warn("WebSocket 연결 인증 실패: 유효하지 않은 토큰");
                    throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
                }
            } else {
                log.warn("WebSocket 연결 인증 실패: Authorization 헤더 없음");
                throw new IllegalArgumentException("인증 정보가 없습니다.");
            }
        }

        return message;
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return authHeader; // Allow passing just the token without Bearer prefix
    }
}
