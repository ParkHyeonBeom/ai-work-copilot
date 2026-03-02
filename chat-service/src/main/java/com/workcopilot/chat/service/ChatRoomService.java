package com.workcopilot.chat.service;

import com.workcopilot.chat.dto.ChatRoomCreateRequest;
import com.workcopilot.chat.dto.ChatRoomDto;
import com.workcopilot.chat.dto.ParticipantDto;
import com.workcopilot.chat.entity.ChatParticipant;
import com.workcopilot.chat.entity.ChatRoom;
import com.workcopilot.chat.entity.ChatRoomType;
import com.workcopilot.chat.repository.ChatMessageRepository;
import com.workcopilot.chat.repository.ChatParticipantRepository;
import com.workcopilot.chat.repository.ChatRoomRepository;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatRoomDto createRoom(Long userId, String userName, ChatRoomCreateRequest request) {
        // For DIRECT rooms, check if an existing room already exists between the two users
        if (request.type() == ChatRoomType.DIRECT && request.memberIds() != null && request.memberIds().size() == 1) {
            Long otherUserId = request.memberIds().get(0);
            List<Long> userIds = List.of(userId, otherUserId);

            List<ChatRoom> existingRooms = chatRoomRepository.findByTypeAndParticipantsUserIdIn(
                    ChatRoomType.DIRECT, userIds, 2L);

            // Find a room that has exactly these 2 participants
            for (ChatRoom room : existingRooms) {
                List<Long> participantIds = room.getParticipants().stream()
                        .map(ChatParticipant::getUserId)
                        .sorted()
                        .toList();
                List<Long> sortedUserIds = userIds.stream().sorted().toList();
                if (participantIds.equals(sortedUserIds)) {
                    log.info("기존 DIRECT 채팅방 반환: roomId={}, users={}", room.getId(), userIds);
                    return toDto(room, userId);
                }
            }
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .name(request.name())
                .type(request.type())
                .creatorUserId(userId)
                .build();

        // Add the creator as a participant
        ChatParticipant creatorParticipant = ChatParticipant.builder()
                .userId(userId)
                .userName(userName)
                .build();
        chatRoom.addParticipant(creatorParticipant);

        // Add other members
        if (request.memberIds() != null) {
            for (Long memberId : request.memberIds()) {
                if (!memberId.equals(userId)) {
                    ChatParticipant participant = ChatParticipant.builder()
                            .userId(memberId)
                            .build();
                    chatRoom.addParticipant(participant);
                }
            }
        }

        chatRoomRepository.save(chatRoom);
        log.info("채팅방 생성: roomId={}, type={}, creator={}", chatRoom.getId(), request.type(), userId);

        return toDto(chatRoom, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomDto> getRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findRoomsByUserId(userId);
        return rooms.stream()
                .map(room -> toDto(room, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatRoomDto getRoom(Long userId, Long roomId) {
        ChatRoom room = chatRoomRepository.findByIdAndParticipantsUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        return toDto(room, userId);
    }

    public void leaveRoom(Long userId, Long roomId) {
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT));

        ChatRoom room = participant.getChatRoom();
        room.removeParticipant(participant);
        chatParticipantRepository.delete(participant);

        log.info("채팅방 퇴장: roomId={}, userId={}", roomId, userId);

        // If no participants left, the room can remain (orphan cleanup can be done later)
    }

    public void inviteMembers(Long userId, Long roomId, List<Long> memberIds) {
        ChatRoom room = chatRoomRepository.findByIdAndParticipantsUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        for (Long memberId : memberIds) {
            if (!chatParticipantRepository.existsByChatRoomIdAndUserId(roomId, memberId)) {
                ChatParticipant participant = ChatParticipant.builder()
                        .userId(memberId)
                        .build();
                room.addParticipant(participant);
            }
        }

        chatRoomRepository.save(room);
        log.info("채팅방 초대: roomId={}, invitedBy={}, members={}", roomId, userId, memberIds);
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findRoomsByUserId(userId);
        int totalUnread = 0;

        for (ChatRoom room : rooms) {
            ChatParticipant participant = room.getParticipants().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            if (participant != null) {
                Long lastReadId = participant.getLastReadMessageId();
                if (lastReadId == null) {
                    lastReadId = 0L;
                }
                totalUnread += (int) chatMessageRepository.countByChatRoomIdAndIdGreaterThan(room.getId(), lastReadId);
            }
        }

        return totalUnread;
    }

    private ChatRoomDto toDto(ChatRoom room, Long currentUserId) {
        List<ParticipantDto> participantDtos = room.getParticipants().stream()
                .map(p -> new ParticipantDto(p.getUserId(), p.getUserName(), p.getLastReadMessageId()))
                .collect(Collectors.toList());

        int unreadCount = 0;
        ChatParticipant currentParticipant = room.getParticipants().stream()
                .filter(p -> p.getUserId().equals(currentUserId))
                .findFirst()
                .orElse(null);

        if (currentParticipant != null) {
            Long lastReadId = currentParticipant.getLastReadMessageId();
            if (lastReadId == null) {
                lastReadId = 0L;
            }
            unreadCount = (int) chatMessageRepository.countByChatRoomIdAndIdGreaterThan(room.getId(), lastReadId);
        }

        return new ChatRoomDto(
                room.getId(),
                room.getName(),
                room.getType(),
                room.getCreatorUserId(),
                room.getLastMessageContent(),
                room.getLastMessageAt(),
                participantDtos,
                unreadCount
        );
    }
}
