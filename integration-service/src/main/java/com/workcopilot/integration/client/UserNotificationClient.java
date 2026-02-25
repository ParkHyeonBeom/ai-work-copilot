package com.workcopilot.integration.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserNotificationClient {

    private final RestTemplate restTemplate;

    @Value("${user-service.url}")
    private String userServiceUrl;

    public void sendMeetingNotification(Long creatorUserId, String meetingTitle,
                                         String meetingTime, String location,
                                         List<String> attendeeEmails) {
        String url = userServiceUrl + "/api/internal/notifications/meeting";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("creatorUserId", creatorUserId);
            body.put("meetingTitle", meetingTitle);
            body.put("meetingTime", meetingTime);
            body.put("location", location != null ? location : "");
            if (attendeeEmails != null) {
                body.put("attendeeEmails", attendeeEmails);
            }

            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
            log.info("회의 알림 전송 완료: creatorUserId={}, title={}, attendees={}",
                    creatorUserId, meetingTitle, attendeeEmails != null ? attendeeEmails.size() : 0);
        } catch (Exception e) {
            log.warn("회의 알림 전송 실패 (무시): creatorUserId={}, error={}", creatorUserId, e.getMessage());
        }
    }
}
