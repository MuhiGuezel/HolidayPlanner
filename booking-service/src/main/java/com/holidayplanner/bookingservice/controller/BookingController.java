package com.holidayplanner.bookingservice.controller;

import com.holidayplanner.shared.model.Booking;
import com.holidayplanner.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // Hello World endpoint
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("BookingService is running!");
    }

    // Get all bookings for an event term
    @GetMapping("/event-term/{eventTermId}")
    public ResponseEntity<List<Booking>> getBookingsForEventTerm(@PathVariable UUID eventTermId) {
        return ResponseEntity.ok(bookingService.getBookingsForEventTerm(eventTermId));
    }

    // Create a booking
    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @RequestParam UUID familyMemberId,
            @RequestParam UUID eventTermId,
            @RequestParam long maxParticipants) {
        return ResponseEntity.ok(bookingService.createBooking(familyMemberId, eventTermId, maxParticipants));
    }

    // Cancel a booking
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Booking> cancelBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }

    // Get confirmed booking count for an event term
    @GetMapping("/event-term/{eventTermId}/count")
    public ResponseEntity<Long> getBookingCount(@PathVariable UUID eventTermId) {
        return ResponseEntity.ok(bookingService.getBookingCount(eventTermId));
    }
}
