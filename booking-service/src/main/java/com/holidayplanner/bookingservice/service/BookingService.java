package com.holidayplanner.bookingservice.service;

import com.holidayplanner.bookingservice.model.Booking;
import com.holidayplanner.bookingservice.model.BookingStatus;
import com.holidayplanner.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    public List<Booking> getBookingsForEventTerm(UUID eventTermId) {
        return bookingRepository.findByEventTermId(eventTermId);
    }

    public Booking createBooking(UUID familyMemberId, UUID eventTermId, long maxParticipants) {
        long confirmedCount = bookingRepository.countByEventTermIdAndStatus(eventTermId, BookingStatus.CONFIRMED);

        Booking booking = new Booking();
        booking.setFamilyMemberId(familyMemberId);
        booking.setEventTermId(eventTermId);
        booking.setStatus(confirmedCount < maxParticipants ? BookingStatus.CONFIRMED : BookingStatus.WAITLISTED);

        return bookingRepository.save(booking);
    }

    public Booking cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Promote first waitlisted booking to confirmed
        promoteFromWaitingList(booking.getEventTermId(), 1);

        return booking;
    }

    public void promoteFromWaitingList(UUID eventTermId, int slots) {
        List<Booking> waitlisted = bookingRepository
                .findByEventTermIdAndStatus(eventTermId, BookingStatus.WAITLISTED);

        waitlisted.stream()
                .limit(slots)
                .forEach(b -> {
                    b.setStatus(BookingStatus.CONFIRMED);
                    bookingRepository.save(b);
                });
    }

    public long getBookingCount(UUID eventTermId) {
        return bookingRepository.countByEventTermIdAndStatus(eventTermId, BookingStatus.CONFIRMED);
    }
}
