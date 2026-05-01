package com.holidayplanner.identityservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events in the Identity Service.
 * 
 * Envelope structure includes:
 * - eventType: semantic event name for consumer routing
 * - timestamp: server time for ordering/replay
 * - eventId: unique per event for idempotency
 * - version: schema version for forward/backward compatibility
 * - payload: event-specific data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {
    
    @JsonProperty("eventType")
    private String eventType;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("eventId")
    private String eventId;
    
    @JsonProperty("version")
    private int version;
    
    @JsonProperty("payload")
    private Object payload;

    /**
     * Factory method to create domain events with standard metadata
     */
    public static DomainEvent of(String eventType, Object payload) {
        DomainEvent event = new DomainEvent();
        event.setEventType(eventType);
        event.setTimestamp(Instant.now());
        event.setEventId(UUID.randomUUID().toString());
        event.setVersion(1);
        event.setPayload(payload);
        return event;
    }
}
