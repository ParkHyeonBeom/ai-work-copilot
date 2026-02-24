package com.workcopilot.common.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class DataSyncRequestedEvent extends DomainEvent {

    private final Long userId;
    private final String syncType; // CALENDAR, GMAIL, DRIVE

    public DataSyncRequestedEvent(Long userId, String syncType) {
        super();
        this.userId = userId;
        this.syncType = syncType;
    }
}
