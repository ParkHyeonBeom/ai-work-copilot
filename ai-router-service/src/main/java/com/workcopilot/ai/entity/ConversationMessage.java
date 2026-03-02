package com.workcopilot.ai.entity;

import com.workcopilot.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "conversation_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConversationMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationHistory conversation;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String contextSources;

    @Column(length = 100)
    private String model;

    private Long processingTimeMs;

    @Builder
    public ConversationMessage(ConversationHistory conversation, String role, String content,
                                String contextSources, String model, Long processingTimeMs) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
        this.contextSources = contextSources;
        this.model = model;
        this.processingTimeMs = processingTimeMs;
    }
}
