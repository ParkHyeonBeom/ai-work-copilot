package com.workcopilot.integration.service;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.integration.dto.EmailDto;
import com.workcopilot.integration.google.GoogleCredentialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailService {

    private static final String USER_ME = "me";

    private final GoogleCredentialProvider credentialProvider;
    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;
    @Qualifier("googleApplicationName")
    private final String applicationName;

    public List<EmailDto> getRecentEmails(Long userId, int maxResults) {
        log.info("최근 이메일 조회: userId={}, maxResults={}", userId, maxResults);

        try {
            Gmail gmail = buildGmailService(userId);

            ListMessagesResponse response = gmail.users().messages()
                    .list(USER_ME)
                    .setMaxResults((long) maxResults)
                    .setLabelIds(List.of("INBOX"))
                    .execute();

            List<Message> messages = response.getMessages();
            if (messages == null || messages.isEmpty()) {
                log.info("조회된 이메일 없음: userId={}", userId);
                return Collections.emptyList();
            }

            List<EmailDto> result = messages.stream()
                    .map(msg -> fetchMessageDetail(gmail, msg.getId()))
                    .collect(Collectors.toList());

            log.info("이메일 조회 완료: userId={}, count={}", userId, result.size());
            return result;

        } catch (IOException e) {
            log.error("Gmail API 호출 실패: userId={}, error={}", userId, e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR,
                    "이메일 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }

    public List<EmailDto> getImportantEmails(Long userId, List<String> importantDomains) {
        log.info("중요 이메일 조회: userId={}, domains={}", userId, importantDomains);

        try {
            Gmail gmail = buildGmailService(userId);

            String query = buildDomainQuery(importantDomains);

            ListMessagesResponse response = gmail.users().messages()
                    .list(USER_ME)
                    .setQ(query)
                    .setMaxResults(20L)
                    .execute();

            List<Message> messages = response.getMessages();
            if (messages == null || messages.isEmpty()) {
                log.info("조회된 중요 이메일 없음: userId={}", userId);
                return Collections.emptyList();
            }

            List<EmailDto> result = messages.stream()
                    .map(msg -> fetchMessageDetail(gmail, msg.getId()))
                    .collect(Collectors.toList());

            log.info("중요 이메일 조회 완료: userId={}, count={}", userId, result.size());
            return result;

        } catch (IOException e) {
            log.error("Gmail API 호출 실패: userId={}, error={}", userId, e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR,
                    "중요 이메일 조회에 실패했습니다: " + e.getMessage());
        }
    }

    public EmailDto getEmailById(Long userId, String messageId) {
        log.info("이메일 상세 조회: userId={}, messageId={}", userId, messageId);

        try {
            Gmail gmail = buildGmailService(userId);
            return fetchMessageDetail(gmail, messageId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gmail API 호출 실패: userId={}, messageId={}, error={}",
                    userId, messageId, e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR,
                    "이메일 상세 조회에 실패했습니다: " + e.getMessage());
        }
    }

    private Gmail buildGmailService(Long userId) {
        return new Gmail.Builder(httpTransport, jsonFactory, credentialProvider.getCredential(userId))
                .setApplicationName(applicationName)
                .build();
    }

    private EmailDto fetchMessageDetail(Gmail gmail, String messageId) {
        try {
            Message message = gmail.users().messages()
                    .get(USER_ME, messageId)
                    .setFormat("metadata")
                    .setMetadataHeaders(List.of("From", "Subject", "Date"))
                    .execute();

            return convertToDto(message);

        } catch (IOException e) {
            log.error("이메일 상세 조회 실패: messageId={}, error={}", messageId, e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR,
                    "이메일 상세 정보를 가져올 수 없습니다: " + e.getMessage());
        }
    }

    private EmailDto convertToDto(Message message) {
        String from = "";
        String subject = "";

        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            for (MessagePartHeader header : message.getPayload().getHeaders()) {
                switch (header.getName()) {
                    case "From" -> from = header.getValue();
                    case "Subject" -> subject = header.getValue();
                }
            }
        }

        LocalDateTime receivedAt = Instant.ofEpochMilli(message.getInternalDate())
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();

        List<String> labels = message.getLabelIds() != null
                ? message.getLabelIds()
                : Collections.emptyList();

        boolean isImportant = labels.contains("IMPORTANT");

        return new EmailDto(
                message.getId(),
                from,
                subject,
                message.getSnippet(),
                receivedAt,
                labels,
                isImportant
        );
    }

    private String buildDomainQuery(List<String> importantDomains) {
        if (importantDomains == null || importantDomains.isEmpty()) {
            return "is:important";
        }

        String domainFilter = importantDomains.stream()
                .map(domain -> "from:" + domain)
                .collect(Collectors.joining(" OR "));

        return "(" + domainFilter + ")";
    }
}
