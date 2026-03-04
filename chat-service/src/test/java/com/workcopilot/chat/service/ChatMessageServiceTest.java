package com.workcopilot.chat.service;

import com.workcopilot.chat.dto.ChatMessageDto;
import com.workcopilot.chat.entity.*;
import com.workcopilot.chat.repository.*;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatParticipantRepository chatParticipantRepository;
    @Mock
    private ChatFileRepository chatFileRepository;
    @Mock
    private ChatReactionRepository chatReactionRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    // ─── editMessage ─────────────────────────────────────

    @Test
    void editMessage_성공_메시지내용수정() {
        ChatRoom room = ChatRoom.builder().build();
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .senderUserId(1L)
                .senderName("테스트")
                .type(ChatMessageType.TEXT)
                .content("원본 메시지")
                .build();
        // Set createdAt via reflection (BaseEntity)
        setCreatedAt(message, LocalDateTime.now().minusMinutes(2));

        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));
        when(chatMessageRepository.save(any())).thenReturn(message);
        when(chatFileRepository.findByChatMessageId(any())).thenReturn(Collections.emptyList());
        when(chatReactionRepository.findByChatMessageId(any())).thenReturn(Collections.emptyList());

        ChatMessageDto result = chatMessageService.editMessage(1L, 10L, "수정된 메시지");

        assertThat(result.content()).isEqualTo("수정된 메시지");
        assertThat(message.getEditedAt()).isNotNull();
        verify(chatMessageRepository).save(message);
    }

    @Test
    void editMessage_타인메시지_CH006에러() {
        ChatMessage message = ChatMessage.builder()
                .senderUserId(2L)
                .type(ChatMessageType.TEXT)
                .content("다른사람 메시지")
                .build();

        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> chatMessageService.editMessage(1L, 10L, "수정"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_NOT_MESSAGE_OWNER);
    }

    @Test
    void editMessage_삭제된메시지_CH005에러() {
        ChatMessage message = ChatMessage.builder()
                .senderUserId(1L)
                .type(ChatMessageType.TEXT)
                .content("삭제될 메시지")
                .build();
        message.markAsDeleted();

        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> chatMessageService.editMessage(1L, 10L, "수정"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MESSAGE_NOT_FOUND);
    }

    @Test
    void editMessage_5분초과_CH007에러() {
        ChatMessage message = ChatMessage.builder()
                .senderUserId(1L)
                .type(ChatMessageType.TEXT)
                .content("오래된 메시지")
                .build();
        setCreatedAt(message, LocalDateTime.now().minusMinutes(10));

        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> chatMessageService.editMessage(1L, 10L, "수정"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_EDIT_TIME_EXPIRED);
    }

    // ─── toggleReaction ──────────────────────────────────

    @Test
    void toggleReaction_추가_새리액션생성() {
        ChatRoom room = ChatRoom.builder().build();
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .senderUserId(2L)
                .type(ChatMessageType.TEXT)
                .content("메시지")
                .build();
        setCreatedAt(message, LocalDateTime.now());

        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));
        when(chatReactionRepository.findByChatMessageIdAndUserIdAndEmoji(10L, 1L, "👍"))
                .thenReturn(Optional.empty());
        when(chatReactionRepository.save(any())).thenReturn(null);
        when(chatReactionRepository.findByChatMessageId(10L)).thenReturn(Collections.emptyList());
        when(chatFileRepository.findByChatMessageId(any())).thenReturn(Collections.emptyList());

        chatMessageService.toggleReaction(1L, "테스트", 10L, "👍");

        verify(chatReactionRepository).save(any(ChatReaction.class));
        verify(chatReactionRepository, never()).delete(any());
    }

    @Test
    void toggleReaction_제거_기존리액션삭제() {
        ChatRoom room = ChatRoom.builder().build();
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .senderUserId(2L)
                .type(ChatMessageType.TEXT)
                .content("메시지")
                .build();
        setCreatedAt(message, LocalDateTime.now());

        ChatReaction existing = ChatReaction.builder()
                .chatMessage(message)
                .userId(1L)
                .userName("테스트")
                .emoji("👍")
                .build();

        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));
        when(chatReactionRepository.findByChatMessageIdAndUserIdAndEmoji(10L, 1L, "👍"))
                .thenReturn(Optional.of(existing));
        when(chatReactionRepository.findByChatMessageId(10L)).thenReturn(Collections.emptyList());
        when(chatFileRepository.findByChatMessageId(any())).thenReturn(Collections.emptyList());

        chatMessageService.toggleReaction(1L, "테스트", 10L, "👍");

        verify(chatReactionRepository).delete(existing);
        verify(chatReactionRepository, never()).save(any());
    }

    // ─── searchMessages ──────────────────────────────────

    @Test
    void searchMessages_비참여자_CH002에러() {
        when(chatParticipantRepository.existsByChatRoomIdAndUserId(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> chatMessageService.searchMessages(1L, 1L, "test", 0, 20))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_NOT_PARTICIPANT);
    }

    @Test
    void searchMessages_키워드매칭_결과반환() {
        ChatRoom room = ChatRoom.builder().build();
        ChatMessage msg = ChatMessage.builder()
                .chatRoom(room)
                .senderUserId(1L)
                .senderName("테스트")
                .type(ChatMessageType.TEXT)
                .content("검색 대상 메시지")
                .build();
        setCreatedAt(msg, LocalDateTime.now());

        when(chatParticipantRepository.existsByChatRoomIdAndUserId(1L, 1L)).thenReturn(true);
        when(chatMessageRepository.searchMessages(eq(1L), eq("검색"), any())).thenReturn(List.of(msg));
        when(chatReactionRepository.findByChatMessageIdIn(any())).thenReturn(Collections.emptyList());
        when(chatFileRepository.findByChatMessageId(any())).thenReturn(Collections.emptyList());

        List<ChatMessageDto> result = chatMessageService.searchMessages(1L, 1L, "검색", 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).contains("검색");
    }

    // ─── extractMentionedUserIds ─────────────────────────

    @Test
    void extractMentionedUserIds_멘션추출_매칭사용자반환() {
        ChatParticipant p1 = ChatParticipant.builder().userId(1L).userName("홍길동").build();
        ChatParticipant p2 = ChatParticipant.builder().userId(2L).userName("김철수").build();

        when(chatParticipantRepository.findByChatRoomId(1L)).thenReturn(List.of(p1, p2));

        Set<Long> result = chatMessageService.extractMentionedUserIds("@홍길동 안녕하세요!", 1L);

        assertThat(result).containsExactly(1L);
    }

    @Test
    void extractMentionedUserIds_멘션없음_빈결과() {
        Set<Long> result = chatMessageService.extractMentionedUserIds("멘션 없는 메시지", 1L);
        assertThat(result).isEmpty();
    }

    @Test
    void extractMentionedUserIds_null입력_빈결과() {
        Set<Long> result = chatMessageService.extractMentionedUserIds(null, 1L);
        assertThat(result).isEmpty();
    }

    // ─── helper ──────────────────────────────────────────

    private void setCreatedAt(Object entity, LocalDateTime time) {
        try {
            var field = entity.getClass().getSuperclass().getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, time);
        } catch (Exception e) {
            throw new RuntimeException("createdAt 설정 실패", e);
        }
    }
}
