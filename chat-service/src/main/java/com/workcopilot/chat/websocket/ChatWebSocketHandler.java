package com.workcopilot.chat.websocket;

import com.workcopilot.chat.dto.ChatMessageDto;
import com.workcopilot.chat.dto.EditMessageRequest;
import com.workcopilot.chat.dto.SendMessageRequest;
import com.workcopilot.chat.dto.ToggleReactionRequest;
import com.workcopilot.chat.entity.ChatParticipant;
import com.workcopilot.chat.repository.ChatParticipantRepository;
import com.workcopilot.chat.repository.ChatRoomRepository;
import com.workcopilot.chat.service.ChatMessageService;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler {

    private final ChatMessageService chatMessageService;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Payload SendMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        StompPrincipal principal = (StompPrincipal) headerAccessor.getUser();
        if (principal == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        Long userId = principal.getUserId();
        String userName = principal.getUserName();

        if (!chatParticipantRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT);
        }

        ChatMessageDto messageDto = chatMessageService.saveMessage(roomId, userId, userName, request);

        // Broadcast to room subscribers
        messagingTemplate.convertAndSend("/topic/room/" + roomId, messageDto);

        // Get room name
        String roomName = chatRoomRepository.findById(roomId)
                .map(r -> r.getName() != null ? r.getName() : "채팅")
                .orElse("채팅");

        // Get participants once
        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomId(roomId);

        // Send notifications + unread updates
        sendNotifications(roomId, userId, userName, roomName, messageDto, participants);
        sendUnreadUpdates(roomId, userId, participants);

        // Send mention notifications
        if (request.content() != null) {
            Set<Long> mentionedUserIds = chatMessageService.extractMentionedUserIds(request.content(), roomId);
            for (Long mentionedUserId : mentionedUserIds) {
                if (mentionedUserId.equals(userId)) continue;
                Map<String, Object> mentionNotification = new HashMap<>();
                mentionNotification.put("type", "MENTION");
                mentionNotification.put("roomId", roomId);
                mentionNotification.put("roomName", roomName);
                mentionNotification.put("senderName", userName != null ? userName : "User");
                String preview = request.content();
                if (preview.length() > 50) preview = preview.substring(0, 50) + "...";
                mentionNotification.put("preview", preview);

                messagingTemplate.convertAndSendToUser(
                        String.valueOf(mentionedUserId),
                        "/queue/notifications",
                        mentionNotification
                );
            }
        }

        log.debug("WebSocket 메시지 전송: roomId={}, sender={}", roomId, userId);
    }

    @MessageMapping("/chat.edit/{roomId}")
    public void editMessage(@DestinationVariable Long roomId,
                            @Payload EditMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        StompPrincipal principal = (StompPrincipal) headerAccessor.getUser();
        if (principal == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        Long userId = principal.getUserId();

        if (!chatParticipantRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT);
        }

        ChatMessageDto editedDto = chatMessageService.editMessage(userId, request.messageId(), request.content());

        Map<String, Object> editEvent = Map.of(
                "type", "EDITED",
                "messageId", request.messageId(),
                "message", editedDto
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, editEvent);

        log.debug("메시지 수정 이벤트: roomId={}, messageId={}", roomId, request.messageId());
    }

    @MessageMapping("/chat.react/{roomId}")
    public void toggleReaction(@DestinationVariable Long roomId,
                               @Payload ToggleReactionRequest request,
                               SimpMessageHeaderAccessor headerAccessor) {
        StompPrincipal principal = (StompPrincipal) headerAccessor.getUser();
        if (principal == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        Long userId = principal.getUserId();
        String userName = principal.getUserName();

        ChatMessageDto updatedDto = chatMessageService.toggleReaction(userId, userName, request.messageId(), request.emoji());

        Map<String, Object> reactionEvent = Map.of(
                "type", "REACTION",
                "messageId", request.messageId(),
                "message", updatedDto
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, reactionEvent);

        log.debug("리액션 이벤트: roomId={}, messageId={}, emoji={}", roomId, request.messageId(), request.emoji());
    }

    @MessageMapping("/chat.delete/{roomId}")
    public void deleteMessage(@DestinationVariable Long roomId,
                              @Payload Map<String, Long> payload,
                              SimpMessageHeaderAccessor headerAccessor) {
        StompPrincipal principal = (StompPrincipal) headerAccessor.getUser();
        if (principal == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        Long userId = principal.getUserId();
        Long messageId = payload.get("messageId");

        ChatMessageDto deletedDto = chatMessageService.deleteMessage(userId, messageId);

        Map<String, Object> deleteEvent = Map.of(
                "type", "DELETED",
                "messageId", messageId,
                "message", deletedDto
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, deleteEvent);

        log.debug("메시지 삭제 이벤트: roomId={}, messageId={}", roomId, messageId);
    }

    @MessageMapping("/chat.typing/{roomId}")
    public void typingIndicator(@DestinationVariable Long roomId,
                                 SimpMessageHeaderAccessor headerAccessor) {
        StompPrincipal principal = (StompPrincipal) headerAccessor.getUser();
        if (principal == null) {
            return;
        }

        Long userId = principal.getUserId();
        String userName = principal.getUserName();

        Map<String, Object> typingEvent = Map.of(
                "userId", userId,
                "userName", userName != null ? userName : "User",
                "typing", true
        );

        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/typing", typingEvent);
    }

    private void sendNotifications(Long roomId, Long senderId, String senderName,
                                    String roomName, ChatMessageDto messageDto,
                                    List<ChatParticipant> participants) {
        String preview = messageDto.content();
        if (preview != null && preview.length() > 50) {
            preview = preview.substring(0, 50) + "...";
        }

        for (ChatParticipant participant : participants) {
            if (participant.getUserId().equals(senderId)) {
                continue;
            }

            Map<String, Object> notification = Map.of(
                    "type", "NEW_MESSAGE",
                    "roomId", roomId,
                    "roomName", roomName,
                    "senderName", senderName != null ? senderName : "User",
                    "preview", preview != null ? preview : ""
            );

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(participant.getUserId()),
                    "/queue/notifications",
                    notification
            );
        }
    }

    private void sendUnreadUpdates(Long roomId, Long senderId, List<ChatParticipant> participants) {
        for (ChatParticipant participant : participants) {
            if (participant.getUserId().equals(senderId)) {
                continue;
            }

            Map<String, Object> unreadEvent = Map.of(
                    "type", "UNREAD_UPDATE",
                    "roomId", roomId
            );

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(participant.getUserId()),
                    "/queue/notifications",
                    unreadEvent
            );
        }
    }
}
