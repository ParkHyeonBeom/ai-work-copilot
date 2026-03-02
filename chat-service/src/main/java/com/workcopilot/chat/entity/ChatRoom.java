package com.workcopilot.chat.entity;

import com.workcopilot.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseEntity {

    @Column
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    @Column(nullable = false)
    private Long creatorUserId;

    @Column(columnDefinition = "TEXT")
    private String lastMessageContent;

    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatParticipant> participants = new ArrayList<>();

    public void addParticipant(ChatParticipant participant) {
        participants.add(participant);
        participant.setChatRoom(this);
    }

    public void removeParticipant(ChatParticipant participant) {
        participants.remove(participant);
        participant.setChatRoom(null);
    }

    public void updateLastMessage(String content, LocalDateTime messageAt) {
        this.lastMessageContent = content;
        this.lastMessageAt = messageAt;
    }
}
