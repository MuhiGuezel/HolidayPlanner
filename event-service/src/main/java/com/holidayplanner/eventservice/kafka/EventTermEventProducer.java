package com.holidayplanner.eventservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.holidayplanner.shared.kafka.KafkaEnvelope;
import com.holidayplanner.shared.kafka.payload.EventTermCancelledPayload;
import com.holidayplanner.shared.kafka.payload.ParticipantListRequestedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventTermEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishEventTermCancelled(EventTermCancelledPayload payload) {
        try {
            KafkaEnvelope<EventTermCancelledPayload> envelope = new KafkaEnvelope<>(
                    "EventTermCancelled", "1",
                    LocalDateTime.now().toString(),
                    "event-service", payload);
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send("holiday-planner.event.term-cancelled",
                    payload.getEventTermId().toString(), json);
        } catch (Exception e) {
            log.error("Failed to publish EventTermCancelled event", e);
        }
    }

    public void publishParticipantListRequested(ParticipantListRequestedPayload payload) {
        try {
            KafkaEnvelope<ParticipantListRequestedPayload> envelope = new KafkaEnvelope<>(
                    "ParticipantListRequested", "1",
                    LocalDateTime.now().toString(),
                    "event-service", payload);
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send("holiday-planner.event.participant-list-requested",
                    payload.getEventTermId().toString(), json);
        } catch (Exception e) {
            log.error("Failed to publish ParticipantListRequested event", e);
        }
    }
}
