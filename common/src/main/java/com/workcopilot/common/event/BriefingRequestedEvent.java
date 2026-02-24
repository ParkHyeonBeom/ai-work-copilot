package com.workcopilot.common.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class BriefingRequestedEvent extends DomainEvent {

    private final Long userId;
    private final String briefingType; // DAILY, MEETING, ADHOC

    public BriefingRequestedEvent(Long userId, String briefingType) {
        super();
        this.userId = userId;
        this.briefingType = briefingType;
    }
}
