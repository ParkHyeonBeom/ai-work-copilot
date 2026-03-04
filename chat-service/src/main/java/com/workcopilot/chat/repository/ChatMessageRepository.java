package com.workcopilot.chat.repository;

import com.workcopilot.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoom.id = :chatRoomId AND (:cursor IS NULL OR m.id < :cursor) ORDER BY m.createdAt DESC")
    List<ChatMessage> findByChatRoomIdWithCursor(
            @Param("chatRoomId") Long chatRoomId,
            @Param("cursor") Long cursor,
            Pageable pageable);

    List<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    long countByChatRoomIdAndIdGreaterThan(Long chatRoomId, Long lastReadMessageId);

    Optional<ChatMessage> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);
}
