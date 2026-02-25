package com.workcopilot.user.service;

import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.user.dto.MeetingNotificationRequest;
import com.workcopilot.user.dto.NotificationResponse;
import com.workcopilot.user.entity.Notification;
import com.workcopilot.user.entity.NotificationType;
import com.workcopilot.user.entity.User;
import com.workcopilot.user.entity.UserStatus;
import com.workcopilot.user.repository.NotificationRepository;
import com.workcopilot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public void notifyTeamMeeting(MeetingNotificationRequest request) {
        User creator = userRepository.findById(request.creatorUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<User> recipients;
        if (request.attendeeEmails() != null && !request.attendeeEmails().isEmpty()) {
            recipients = userRepository.findByEmailInAndStatus(request.attendeeEmails(), UserStatus.ACTIVE);
        } else if (creator.getDepartment() != null) {
            recipients = userRepository.findByDepartmentAndStatus(creator.getDepartment(), UserStatus.ACTIVE);
        } else {
            log.warn("알림 대상 없음: userId={}", creator.getId());
            return;
        }

        String title = "새 회의 일정: " + request.meetingTitle();
        String message = String.format("%s님이 회의를 생성했습니다.\n일시: %s%s",
                creator.getName(),
                request.meetingTime(),
                request.location() != null ? "\n장소: " + request.location() : "");

        int count = 0;
        for (User recipient : recipients) {
            if (recipient.getId().equals(creator.getId())) continue;

            Notification notification = Notification.builder()
                    .userId(recipient.getId())
                    .type(NotificationType.MEETING_INVITE)
                    .title(title)
                    .message(message)
                    .build();
            notificationRepository.save(notification);

            emailService.sendMeetingNotification(recipient.getEmail(), title, message);
            count++;
        }

        log.info("회의 알림 발송 완료: creator={}, recipients={}", creator.getName(), count);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(Long userId) {
        return notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
