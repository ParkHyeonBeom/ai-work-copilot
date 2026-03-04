package com.workcopilot.ai.entity;

import com.workcopilot.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "conversation_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConversationHistory extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(length = 200)
    private String title;

    @Column(nullable = false)
    private boolean isActive = true;

    @Builder
    public ConversationHistory(Long userId, String title) {
        this.userId = userId;
        this.title = title;
        this.isActive = true;
    }

    public void updateTitle(String title) {
        if (title != null && title.length() > 200) {
            this.title = title.substring(0, 200);
        } else {
            this.title = title;
        }
    }

    public void deactivate() {
        this.isActive = false;
    }
}
