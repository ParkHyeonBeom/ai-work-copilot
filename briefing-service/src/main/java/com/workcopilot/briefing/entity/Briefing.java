package com.workcopilot.briefing.entity;

import com.workcopilot.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "briefings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Briefing extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate briefingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BriefingStatus status;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String fullContent;

    @ElementCollection
    @CollectionTable(name = "briefing_key_points", joinColumns = @JoinColumn(name = "briefing_id"))
    @Column(name = "key_point")
    @Builder.Default
    private List<String> keyPoints = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "briefing_action_items", joinColumns = @JoinColumn(name = "briefing_id"))
    @Column(name = "action_item")
    @Builder.Default
    private List<String> actionItems = new ArrayList<>();

    private int eventCount;

    private int emailCount;

    private int fileCount;

    private LocalDateTime completedAt;

    public void updateStatus(BriefingStatus status) {
        this.status = status;
    }

    public void complete(String summary, String fullContent, List<String> keyPoints,
                         List<String> actionItems, int eventCount, int emailCount, int fileCount) {
        this.status = BriefingStatus.COMPLETED;
        this.summary = summary;
        this.fullContent = fullContent;
        this.keyPoints = keyPoints != null ? keyPoints : new ArrayList<>();
        this.actionItems = actionItems != null ? actionItems : new ArrayList<>();
        this.eventCount = eventCount;
        this.emailCount = emailCount;
        this.fileCount = fileCount;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = BriefingStatus.FAILED;
        this.summary = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
}
