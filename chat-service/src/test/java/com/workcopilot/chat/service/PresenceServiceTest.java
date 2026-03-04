package com.workcopilot.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PresenceServiceTest {

    private PresenceService presenceService;

    @BeforeEach
    void setUp() {
        presenceService = new PresenceService();
    }

    @Test
    void userConnected_접속후_온라인() {
        presenceService.userConnected(1L, "session-1");

        assertThat(presenceService.isOnline(1L)).isTrue();
        assertThat(presenceService.getOnlineUserIds()).contains(1L);
    }

    @Test
    void userDisconnected_해제후_오프라인() {
        presenceService.userConnected(1L, "session-1");
        presenceService.userDisconnected(1L, "session-1");

        assertThat(presenceService.isOnline(1L)).isFalse();
        assertThat(presenceService.getOnlineUserIds()).doesNotContain(1L);
    }

    @Test
    void 다중세션_하나해제후_여전히온라인() {
        presenceService.userConnected(1L, "session-1");
        presenceService.userConnected(1L, "session-2");

        presenceService.userDisconnected(1L, "session-1");

        assertThat(presenceService.isOnline(1L)).isTrue();
    }

    @Test
    void 다중세션_모두해제후_오프라인() {
        presenceService.userConnected(1L, "session-1");
        presenceService.userConnected(1L, "session-2");

        presenceService.userDisconnected(1L, "session-1");
        presenceService.userDisconnected(1L, "session-2");

        assertThat(presenceService.isOnline(1L)).isFalse();
    }

    @Test
    void getOnlineUserIds_여러사용자_목록반환() {
        presenceService.userConnected(1L, "s1");
        presenceService.userConnected(2L, "s2");
        presenceService.userConnected(3L, "s3");

        Set<Long> online = presenceService.getOnlineUserIds();

        assertThat(online).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void isOnline_미접속사용자_오프라인() {
        assertThat(presenceService.isOnline(999L)).isFalse();
    }
}
