package com.workcopilot.chat.service;

import com.workcopilot.chat.dto.*;
import com.workcopilot.chat.entity.*;
import com.workcopilot.chat.repository.*;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatMessageService {

    private static final int EDIT_TIME_LIMIT_MINUTES = 5;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\S+)");

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatFileRepository chatFileRepository;
    private final ChatReactionRepository chatReactionRepository;

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessages(Long userId, Long roomId, Long cursor, int size) {
        if (!chatParticipantRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT);
        }

        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdWithCursor(
                roomId, cursor, PageRequest.of(0, size));

        // Batch load reactions to prevent N+1
        List<Long> messageIds = messages.stream().map(ChatMessage::getId).toList();
        Map<Long, List<ChatReaction>> reactionsMap = chatReactionRepository.findByChatMessageIdIn(messageIds)
                .stream().collect(Collectors.groupingBy(r -> r.getChatMessage().getId()));

        List<ChatMessageDto> result = messages.stream()
                .map(m -> toDto(m, reactionsMap.getOrDefault(m.getId(), Collections.emptyList()), userId))
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

        return toDto(message, Collections.emptyList(), senderUserId);
    }

    public ChatMessageDto editMessage(Long userId, Long messageId, String newContent) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        if (!message.getSenderUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_MESSAGE_OWNER);
        }

        if (message.isDeleted()) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
        }

        if (message.getCreatedAt().plusMinutes(EDIT_TIME_LIMIT_MINUTES).isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.CHAT_EDIT_TIME_EXPIRED);
        }

        message.editContent(newContent);
        chatMessageRepository.save(message);

        log.info("메시지 수정: messageId={}, userId={}", messageId, userId);

        List<ChatReaction> reactions = chatReactionRepository.findByChatMessageId(messageId);
        return toDto(message, reactions, userId);
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

        return toDto(message, Collections.emptyList(), userId);
    }

    public ChatMessageDto toggleReaction(Long userId, String userName, Long messageId, String emoji) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        Optional<ChatReaction> existing = chatReactionRepository
                .findByChatMessageIdAndUserIdAndEmoji(messageId, userId, emoji);

        if (existing.isPresent()) {
            chatReactionRepository.delete(existing.get());
            log.debug("리액션 제거: messageId={}, userId={}, emoji={}", messageId, userId, emoji);
        } else {
            ChatReaction reaction = ChatReaction.builder()
                    .chatMessage(message)
                    .userId(userId)
                    .userName(userName)
                    .emoji(emoji)
                    .build();
            chatReactionRepository.save(reaction);
            log.debug("리액션 추가: messageId={}, userId={}, emoji={}", messageId, userId, emoji);
        }

        List<ChatReaction> reactions = chatReactionRepository.findByChatMessageId(messageId);
        return toDto(message, reactions, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> searchMessages(Long userId, Long roomId, String keyword, int page, int size) {
        if (!chatParticipantRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT);
        }

        List<ChatMessage> messages = chatMessageRepository.searchMessages(
                roomId, keyword, PageRequest.of(page, size));

        List<Long> messageIds = messages.stream().map(ChatMessage::getId).toList();
        Map<Long, List<ChatReaction>> reactionsMap = chatReactionRepository.findByChatMessageIdIn(messageIds)
                .stream().collect(Collectors.groupingBy(r -> r.getChatMessage().getId()));

        return messages.stream()
                .map(m -> toDto(m, reactionsMap.getOrDefault(m.getId(), Collections.emptyList()), userId))
                .toList();
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

    public Set<Long> extractMentionedUserIds(String content, Long roomId) {
        if (content == null || content.isBlank()) return Collections.emptySet();

        Matcher matcher = MENTION_PATTERN.matcher(content);
        Set<String> mentionedNames = new HashSet<>();
        while (matcher.find()) {
            mentionedNames.add(matcher.group(1));
        }

        if (mentionedNames.isEmpty()) return Collections.emptySet();

        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomId(roomId);
        return participants.stream()
                .filter(p -> mentionedNames.contains(p.getUserName()))
                .map(ChatParticipant::getUserId)
                .collect(Collectors.toSet());
    }

    private ChatMessageDto toDto(ChatMessage message, List<ChatReaction> reactions, Long currentUserId) {
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

        // Build reaction DTOs grouped by emoji
        List<ReactionDto> reactionDtos = Collections.emptyList();
        if (reactions != null && !reactions.isEmpty()) {
            Map<String, List<ChatReaction>> grouped = reactions.stream()
                    .collect(Collectors.groupingBy(ChatReaction::getEmoji));
            reactionDtos = grouped.entrySet().stream()
                    .map(entry -> new ReactionDto(
                            entry.getKey(),
                            entry.getValue().size(),
                            entry.getValue().stream()
                                    .map(r -> new ReactionUserDto(r.getUserId(), r.getUserName()))
                                    .toList(),
                            entry.getValue().stream().anyMatch(r -> r.getUserId().equals(currentUserId))
                    ))
                    .toList();
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
                reactionDtos,
                message.getEditedAt(),
                message.getCreatedAt()
        );
    }
}
