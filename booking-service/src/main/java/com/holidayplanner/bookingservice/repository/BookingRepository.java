package com.holidayplanner.bookingservice.repository;

import com.holidayplanner.bookingservice.model.Booking;
import com.holidayplanner.bookingservice.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByEventTermId(UUID eventTermId);

    List<Booking> findByEventTermIdAndStatus(UUID eventTermId, BookingStatus status);

    List<Booking> findByFamilyMemberId(UUID familyMemberId);

    long countByEventTermIdAndStatus(UUID eventTermId, BookingStatus status);
}
