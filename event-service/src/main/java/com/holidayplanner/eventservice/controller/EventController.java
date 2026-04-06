package com.holidayplanner.eventservice.controller;

import com.holidayplanner.eventservice.model.*;
import com.holidayplanner.eventservice.service.EventManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventManagementService eventManagementService;

    // Hello World endpoint
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("EventService is running!");
    }

    // Get all events for an organization
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<Event>> getEventsByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(eventManagementService.getEventsByOrganization(organizationId));
    }

    // Update an event
    @PutMapping("/{eventId}")
    public ResponseEntity<Event> updateEvent(
            @PathVariable UUID eventId,
            @RequestParam String shortTitle,
            @RequestParam String description,
            @RequestParam String location,
            @RequestParam(required = false) String meetingPoint,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam int minimalAge,
            @RequestParam int maximalAge,
            @RequestParam(required = false) String pictureUrl) {
        return ResponseEntity.ok(eventManagementService.updateEvent(
                eventId, shortTitle, description, location, meetingPoint,
                price, paymentMethod, minimalAge, maximalAge, pictureUrl));
    }

    // Create an event term
    @PostMapping("/{eventId}/terms")
    public ResponseEntity<EventTerm> createEventTerm(
            @PathVariable UUID eventId,
            @RequestParam LocalDateTime startDateTime,
            @RequestParam LocalDateTime endDateTime,
            @RequestParam int minParticipants,
            @RequestParam int maxParticipants) {
        return ResponseEntity.ok(eventManagementService.createEventTerm(
                eventId, startDateTime, endDateTime, minParticipants, maxParticipants));
    }

    // Change event term status
    @PatchMapping("/terms/{eventTermId}/status")
    public ResponseEntity<EventTerm> changeEventTermStatus(
            @PathVariable UUID eventTermId,
            @RequestParam EventTermStatus newStatus) {
        return ResponseEntity.ok(eventManagementService.changeEventTermStatus(eventTermId, newStatus));
    }

    // Update event term capacity
    @PatchMapping("/terms/{eventTermId}/capacity")
    public ResponseEntity<EventTerm> updateEventTermCapacity(
            @PathVariable UUID eventTermId,
            @RequestParam int minParticipants,
            @RequestParam int maxParticipants) {
        return ResponseEntity.ok(eventManagementService.updateEventTermCapacity(
                eventTermId, minParticipants, maxParticipants));
    }

    // Assign caregiver to event term
    @PostMapping("/terms/{eventTermId}/caregivers/{caregiverId}")
    public ResponseEntity<EventTerm> assignCaregiver(
            @PathVariable UUID eventTermId,
            @PathVariable UUID caregiverId) {
        return ResponseEntity.ok(eventManagementService.assignCaregiverToEventTerm(eventTermId, caregiverId));
    }

    // Create a remark
    @PostMapping("/terms/{eventTermId}/remarks")
    public ResponseEntity<Remark> createRemark(
            @PathVariable UUID eventTermId,
            @RequestParam UUID familyMemberId,
            @RequestParam UUID eventOwnerId,
            @RequestParam String description) {
        return ResponseEntity.ok(eventManagementService.createRemark(
                eventTermId, familyMemberId, eventOwnerId, description));
    }

    // Get remarks for an event term
    @GetMapping("/terms/{eventTermId}/remarks")
    public ResponseEntity<List<Remark>> getRemarks(@PathVariable UUID eventTermId) {
        return ResponseEntity.ok(eventManagementService.getRemarks(eventTermId));
    }
}
