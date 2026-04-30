package com.holidayplanner.eventservice.service;

import com.holidayplanner.eventservice.kafka.EventTermEventProducer;
import com.holidayplanner.shared.kafka.payload.EventTermCancelledPayload;
import com.holidayplanner.shared.model.*;
import com.holidayplanner.eventservice.repository.EventRepository;
import com.holidayplanner.eventservice.repository.EventTermRepository;
import com.holidayplanner.eventservice.repository.RemarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class EventManagementService {

    private final EventRepository eventRepository;
    private final EventTermRepository eventTermRepository;
    private final RemarkRepository remarkRepository;
    private final EventTermEventProducer eventTermEventProducer;

    // --- Event Operations ---

    public Event createEvent(UUID organizationId, UUID eventOwnerId, String shortTitle,
                             String description, String location, int minimalAge, int maximalAge) {
        Event event = new Event();
        event.setOrganizationId(organizationId);
        event.setEventOwnerId(eventOwnerId);
        event.setShortTitle(shortTitle);
        event.setDescription(description);
        event.setLocation(location);
        event.setMinimalAge(minimalAge);
        event.setMaximalAge(maximalAge);
        return eventRepository.save(event);
    }

    public Event updateEvent(UUID eventId, String shortTitle, String description,
                             String location, String meetingPoint, BigDecimal price,
                             PaymentMethod paymentMethod, int minimalAge, int maximalAge,
                             String pictureUrl) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        event.setShortTitle(shortTitle);
        event.setDescription(description);
        event.setLocation(location);
        event.setMeetingPoint(meetingPoint);
        event.setPrice(price);
        event.setPaymentMethod(paymentMethod);
        event.setMinimalAge(minimalAge);
        event.setMaximalAge(maximalAge);
        event.setPictureUrl(pictureUrl);

        return eventRepository.save(event);
    }

    public List<Event> getEventsByOrganization(UUID organizationId) {
        return eventRepository.findByOrganizationId(organizationId);
    }

    // --- EventTerm Operations ---

    public EventTerm createEventTerm(UUID eventId, LocalDateTime startDateTime,
                                     LocalDateTime endDateTime, int minParticipants,
                                     int maxParticipants) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        EventTerm term = new EventTerm();
        term.setEvent(event);
        term.setStartDateTime(startDateTime);
        term.setEndDateTime(endDateTime);
        term.setMinParticipants(minParticipants);
        term.setMaxParticipants(maxParticipants);
        term.setStatus(EventTermStatus.DRAFT);

        return eventTermRepository.save(term);
    }

    public EventTerm changeEventTermStatus(UUID eventTermId, EventTermStatus newStatus) {
        EventTerm term = eventTermRepository.findByIdWithEvent(eventTermId)
                .orElseThrow(() -> new RuntimeException("EventTerm not found: " + eventTermId));

        term.setStatus(newStatus);
        EventTerm saved = eventTermRepository.save(term);

        if (newStatus == EventTermStatus.CANCELLED) {
            List<String> caregiverIds = term.getCaregiverIds().stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());
            EventTermCancelledPayload payload = new EventTermCancelledPayload(
                    term.getId(),
                    term.getEvent() != null ? term.getEvent().getShortTitle() : null,
                    term.getStartDateTime() != null ? term.getStartDateTime().toString() : null,
                    term.getEvent() != null ? term.getEvent().getOrganizationId() : null,
                    caregiverIds,
                    "event-owner");
            eventTermEventProducer.publishEventTermCancelled(payload);
        }

        return saved;
    }

    public EventTerm updateEventTermCapacity(UUID eventTermId, int minParticipants, int maxParticipants) {
        EventTerm term = eventTermRepository.findById(eventTermId)
                .orElseThrow(() -> new RuntimeException("EventTerm not found: " + eventTermId));

        term.setMinParticipants(minParticipants);
        term.setMaxParticipants(maxParticipants);
        return eventTermRepository.save(term);
        // Note: if max increased → BookingService::promoteFromWaitingList + NotificationService::notifyBookingConfirmed
    }

    public EventTerm assignCaregiverToEventTerm(UUID eventTermId, UUID caregiverId) {
        EventTerm term = eventTermRepository.findById(eventTermId)
                .orElseThrow(() -> new RuntimeException("EventTerm not found: " + eventTermId));

        if (!term.getCaregiverIds().contains(caregiverId)) {
            term.getCaregiverIds().add(caregiverId);
        }
        return eventTermRepository.save(term);
    }

    public EventTerm verifyEventTerm(UUID eventTermId) {
        return eventTermRepository.findByIdWithEvent(eventTermId)
                .orElseThrow(() -> new RuntimeException("EventTerm not found: " + eventTermId));
    }

    // --- Remark Operations ---

    public Remark createRemark(UUID eventTermId, UUID familyMemberId,
                               UUID eventOwnerId, String description) {
        // Verify term exists
        eventTermRepository.findById(eventTermId)
                .orElseThrow(() -> new RuntimeException("EventTerm not found: " + eventTermId));

        Remark remark = new Remark();
        remark.setEventTermId(eventTermId);
        remark.setFamilyMemberId(familyMemberId);
        remark.setEventOwnerId(eventOwnerId);
        remark.setDescription(description);

        return remarkRepository.save(remark);
    }

    public List<Remark> getRemarks(UUID eventTermId) {
        return remarkRepository.findByEventTermId(eventTermId);
    }
}
