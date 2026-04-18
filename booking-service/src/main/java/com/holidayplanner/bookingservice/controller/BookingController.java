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

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("BookingService is running!");
    }

    @GetMapping("/event-term/{eventTermId}")
    public ResponseEntity<List<Booking>> getBookingsForEventTerm(@PathVariable UUID eventTermId) {
        return ResponseEntity.ok(bookingService.getBookingsForEventTerm(eventTermId));
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @RequestParam UUID familyMemberId,
            @RequestParam UUID eventTermId) {
        return ResponseEntity.ok(bookingService.createBooking(familyMemberId, eventTermId));
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Booking> cancelBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }

    @GetMapping("/event-term/{eventTermId}/count")
    public ResponseEntity<Long> getBookingCount(@PathVariable UUID eventTermId) {
        return ResponseEntity.ok(bookingService.getBookingCount(eventTermId));
    }
}
