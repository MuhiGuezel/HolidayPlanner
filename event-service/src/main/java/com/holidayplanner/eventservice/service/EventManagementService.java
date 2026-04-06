package com.holidayplanner.eventservice.service;

import com.holidayplanner.eventservice.model.*;
import com.holidayplanner.eventservice.repository.EventRepository;
import com.holidayplanner.eventservice.repository.EventTermRepository;
import com.holidayplanner.eventservice.repository.RemarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventManagementService {

    private final EventRepository eventRepository;
    private final EventTermRepository eventTermRepository;
    private final RemarkRepository remarkRepository;

    // --- Event Operations ---

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
        EventTerm term = eventTermRepository.findById(eventTermId)
                .orElseThrow(() -> new RuntimeException("EventTerm not found: " + eventTermId));

        term.setStatus(newStatus);
        return eventTermRepository.save(term);
        // Note: if CANCELLED → BookingService::cancelAllBookings + NotificationService::notifyTermCancelled
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
        return eventTermRepository.findById(eventTermId)
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
