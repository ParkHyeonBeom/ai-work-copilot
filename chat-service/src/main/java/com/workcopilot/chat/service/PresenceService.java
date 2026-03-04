package com.workcopilot.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PresenceService {

    private final ConcurrentHashMap<Long, Set<String>> onlineUsers = new ConcurrentHashMap<>();

    public void userConnected(Long userId, String sessionId) {
        onlineUsers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.debug("사용자 접속: userId={}, sessionId={}, sessions={}", userId, sessionId, onlineUsers.get(userId).size());
    }

    public void userDisconnected(Long userId, String sessionId) {
        Set<String> sessions = onlineUsers.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                onlineUsers.remove(userId);
            }
        }
        log.debug("사용자 해제: userId={}, sessionId={}", userId, sessionId);
    }

    public boolean isOnline(Long userId) {
        Set<String> sessions = onlineUsers.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public Set<Long> getOnlineUserIds() {
        return Collections.unmodifiableSet(onlineUsers.keySet());
    }
}
