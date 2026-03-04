package com.workcopilot.chat.entity;

import com.workcopilot.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(nullable = false)
    private Long senderUserId;

    private String senderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMessageType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    private boolean deleted = false;

    private Long replyToMessageId;

    private LocalDateTime editedAt;

    public void markAsDeleted() {
        this.deleted = true;
        this.content = null;
    }

    public void editContent(String newContent) {
        this.content = newContent;
        this.editedAt = LocalDateTime.now();
    }
}
