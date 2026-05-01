package com.holidayplanner.identityservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * Service for publishing domain events to Kafka topics.
 * 
 * Handles:
 * - UserRegisteredEvent → identity.user.registered
 * - UserPhoneUpdatedEvent → identity.user.phone_updated
 * - FamilyMemberAddedEvent → identity.family_member.added
 * - FamilyMemberRemovedEvent → identity.family_member.removed
 * 
 * All events wrapped in DomainEvent envelope with:
 * - eventId: unique for idempotency
 * - timestamp: server time
 * - version: schema version
 * 
 * Message keys use pattern: {entityType}:{entityId}
 * This ensures ordering per entity (partitioned by key).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish UserRegisteredEvent
     */
    public void publishUserRegistered(UserRegisteredEvent payload) {
        String topic = "identity.user.registered";
        String key = "user:" + payload.getUserId();
        DomainEvent event = DomainEvent.of("UserRegistered", payload);
        publishEvent(topic, key, event);
        log.info("Published UserRegisteredEvent for user {} to topic {}", payload.getUserId(), topic);
    }

    /**
     * Publish UserPhoneUpdatedEvent
     */
    public void publishUserPhoneUpdated(UserPhoneUpdatedEvent payload) {
        String topic = "identity.user.phone_updated";
        String key = "user:" + payload.getUserId();
        DomainEvent event = DomainEvent.of("UserPhoneUpdated", payload);
        publishEvent(topic, key, event);
        log.info("Published UserPhoneUpdatedEvent for user {} to topic {}", payload.getUserId(), topic);
    }

    /**
     * Publish FamilyMemberAddedEvent
     */
    public void publishFamilyMemberAdded(FamilyMemberAddedEvent payload) {
        String topic = "identity.family_member.added";
        String key = "family_member:" + payload.getFamilyMemberId();
        DomainEvent event = DomainEvent.of("FamilyMemberAdded", payload);
        publishEvent(topic, key, event);
        log.info("Published FamilyMemberAddedEvent for member {} to topic {}", payload.getFamilyMemberId(), topic);
    }

    /**
     * Publish FamilyMemberRemovedEvent
     */
    public void publishFamilyMemberRemoved(FamilyMemberRemovedEvent payload) {
        String topic = "identity.family_member.removed";
        String key = "family_member:" + payload.getFamilyMemberId();
        DomainEvent event = DomainEvent.of("FamilyMemberRemoved", payload);
        publishEvent(topic, key, event);
        log.info("Published FamilyMemberRemovedEvent for member {} to topic {}", payload.getFamilyMemberId(), topic);
    }

    /**
     * Generic event publishing with error handling
     */
    private void publishEvent(String topic, String key, DomainEvent event) {
        try {
            Message<DomainEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, key)
                    .build();

            kafkaTemplate.send(message);
            log.debug("Event published to topic {} with key {}: eventId={}", topic, key, event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish event to topic {}: {}", topic, e.getMessage(), e);
            // In production, consider: retry logic, dead letter queue, alerting
        }
    }
}
