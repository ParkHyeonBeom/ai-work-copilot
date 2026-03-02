package com.workcopilot.chat.entity;

import com.workcopilot.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chat_room_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    @Setter
    private ChatRoom chatRoom;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    private Long lastReadMessageId;

    public void updateLastReadMessageId(Long messageId) {
        this.lastReadMessageId = messageId;
    }
}
