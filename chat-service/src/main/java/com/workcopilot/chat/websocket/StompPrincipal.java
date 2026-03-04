package com.workcopilot.chat.websocket;

import java.security.Principal;

public class StompPrincipal implements Principal {

    private final Long userId;
    private final String userName;

    public StompPrincipal(Long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
