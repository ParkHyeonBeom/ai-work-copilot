package com.workcopilot.common.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@SuperBuilder
public abstract class DomainEvent {

    private final String eventId;
    private final String eventType;
    private final LocalDateTime occurredAt;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.occurredAt = LocalDateTime.now();
    }
}
