package com.holidayplanner.paymentservice.repository;

import com.holidayplanner.paymentservice.model.Payment;
import com.holidayplanner.paymentservice.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBookingId(UUID bookingId);
    List<Payment> findByOrganizationId(UUID organizationId);
    List<Payment> findByOrganizationIdAndStatus(UUID organizationId, PaymentStatus status);
}
