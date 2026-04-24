package com.holidayplanner.bookingservice.service;

import com.holidayplanner.bookingservice.client.EventServiceClient;
import com.holidayplanner.bookingservice.client.EventTermDetails;
import com.holidayplanner.bookingservice.exception.BookingNotFoundException;
import com.holidayplanner.shared.model.Booking;
import com.holidayplanner.shared.model.BookingStatus;
import com.holidayplanner.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventServiceClient eventServiceClient;

    public List<Booking> getBookingsForEventTerm(UUID eventTermId) {
        return bookingRepository.findByEventTermId(eventTermId);
    }

    public Booking createBooking(UUID familyMemberId, UUID eventTermId) {
        EventTermDetails eventTerm = eventServiceClient.getEventTerm(eventTermId);

        if (!"ACTIVE".equals(eventTerm.getStatus())) {
            throw new IllegalStateException("Event term is not active: " + eventTermId);
        }

        long confirmedCount = bookingRepository.countByEventTermIdAndStatus(eventTermId, BookingStatus.CONFIRMED);

        Booking booking = new Booking();
        booking.setFamilyMemberId(familyMemberId);
        booking.setEventTermId(eventTermId);
        booking.setStatus(confirmedCount < eventTerm.getMaxParticipants()
                ? BookingStatus.CONFIRMED
                : BookingStatus.WAITLISTED);

        return bookingRepository.save(booking);
    }

    public Booking cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        UUID eventTermId = booking.getEventTermId();
        if (eventTermId != null) {
            promoteFromWaitingList(eventTermId, 1);
        }

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
