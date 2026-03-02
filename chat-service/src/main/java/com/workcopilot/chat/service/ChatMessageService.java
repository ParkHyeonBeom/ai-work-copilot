package com.workcopilot.chat.service;

import com.workcopilot.chat.dto.ChatFileDto;
import com.workcopilot.chat.dto.ChatMessageDto;
import com.workcopilot.chat.dto.ReplyMessageDto;
import com.workcopilot.chat.dto.SendMessageRequest;
import com.workcopilot.chat.entity.ChatFile;
import com.workcopilot.chat.entity.ChatMessage;
import com.workcopilot.chat.entity.ChatParticipant;
import com.workcopilot.chat.entity.ChatRoom;
import com.workcopilot.chat.repository.ChatFileRepository;
import com.workcopilot.chat.repository.ChatMessageRepository;
import com.workcopilot.chat.repository.ChatParticipantRepository;
import com.workcopilot.chat.repository.ChatRoomRepository;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatFileRepository chatFileRepository;

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessages(Long userId, Long roomId, Long cursor, int size) {
        if (!chatParticipantRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT);
        }

        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdWithCursor(
                roomId, cursor, PageRequest.of(0, size));

        List<ChatMessageDto> result = messages.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        Collections.reverse(result);
        return result;
    }

    public ChatMessageDto saveMessage(Long roomId, Long senderUserId, String senderName, SendMessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .senderUserId(senderUserId)
                .senderName(senderName)
                .type(request.type())
                .content(request.content())
                .replyToMessageId(request.replyToMessageId())
                .build();

        chatMessageRepository.save(message);

        chatRoom.updateLastMessage(request.content(), LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        log.debug("메시지 저장: roomId={}, sender={}, type={}", roomId, senderUserId, request.type());

        return toDto(message);
    }

    public ChatMessageDto deleteMessage(Long userId, Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        if (!message.getSenderUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_MESSAGE_OWNER);
        }

        message.markAsDeleted();
        chatMessageRepository.save(message);

        log.info("메시지 삭제: messageId={}, userId={}", messageId, userId);

        return toDto(message);
    }

    public void markAsRead(Long userId, Long roomId) {
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT));

        chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(roomId)
                .ifPresent(latestMessage -> {
                    participant.updateLastReadMessageId(latestMessage.getId());
                    chatParticipantRepository.save(participant);
                    log.debug("읽음 처리: roomId={}, userId={}, lastReadMessageId={}", roomId, userId, latestMessage.getId());
                });
    }

    private ChatMessageDto toDto(ChatMessage message) {
        ChatFileDto fileDto = null;
        List<ChatFile> files = chatFileRepository.findByChatMessageId(message.getId());
        if (!files.isEmpty()) {
            ChatFile file = files.get(0);
            fileDto = new ChatFileDto(file.getId(), file.getOriginalFileName(), file.getMimeType(), file.getFileSize());
        }

        ReplyMessageDto replyDto = null;
        if (message.getReplyToMessageId() != null) {
            replyDto = chatMessageRepository.findById(message.getReplyToMessageId())
                    .map(replyMsg -> new ReplyMessageDto(
                            replyMsg.getId(),
                            replyMsg.getSenderName(),
                            replyMsg.isDeleted() ? "삭제된 메시지입니다" :
                                    (replyMsg.getContent() != null && replyMsg.getContent().length() > 50
                                            ? replyMsg.getContent().substring(0, 50) + "..."
                                            : replyMsg.getContent())
                    ))
                    .orElse(null);
        }

        return new ChatMessageDto(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSenderUserId(),
                message.getSenderName(),
                message.getType(),
                message.isDeleted() ? "삭제된 메시지입니다" : message.getContent(),
                fileDto,
                message.isDeleted(),
                replyDto,
                message.getCreatedAt()
        );
    }
}
