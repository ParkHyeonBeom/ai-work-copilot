package com.workcopilot.chat.repository;

import com.workcopilot.chat.entity.ChatRoom;
import com.workcopilot.chat.entity.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p WHERE cr.id = :roomId AND p.userId = :userId")
    Optional<ChatRoom> findByIdAndParticipantsUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
            "LEFT JOIN FETCH cr.participants p " +
            "WHERE cr.id IN (SELECT cr2.id FROM ChatRoom cr2 JOIN cr2.participants p2 WHERE p2.userId = :userId) " +
            "ORDER BY cr.lastMessageAt DESC NULLS LAST")
    List<ChatRoom> findRoomsByUserId(@Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p " +
            "WHERE cr.type = :type AND p.userId IN :userIds " +
            "GROUP BY cr HAVING COUNT(p) = :userCount")
    List<ChatRoom> findByTypeAndParticipantsUserIdIn(
            @Param("type") ChatRoomType type,
            @Param("userIds") List<Long> userIds,
            @Param("userCount") long userCount);
}
