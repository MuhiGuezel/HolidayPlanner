package com.holidayplanner.paymentservice.service;

import com.holidayplanner.paymentservice.kafka.PaymentEventProducer;
import com.holidayplanner.shared.kafka.payload.PaymentRefundedPayload;
import com.holidayplanner.shared.model.Payment;
import com.holidayplanner.shared.model.PaymentStatus;
import com.holidayplanner.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;

    public Payment createPayment(UUID bookingId, UUID organizationId, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setOrganizationId(organizationId);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepository.save(payment);
    }

    public Payment markAsPaid(UUID paymentId, String note) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setNote(note);
        return paymentRepository.save(payment);
    }

    public Payment refundPayment(UUID paymentId, String note) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setNote(note);
        Payment saved = paymentRepository.save(payment);

        PaymentRefundedPayload payload = new PaymentRefundedPayload(
                saved.getId(), saved.getBookingId(), saved.getOrganizationId(),
                null, null, saved.getAmount());
        paymentEventProducer.publishPaymentRefunded(payload);

        return saved;
    }

    public List<Payment> getPaymentsByOrganization(UUID organizationId) {
        return paymentRepository.findByOrganizationId(organizationId);
    }

    public List<Payment> getPendingPayments(UUID organizationId) {
        return paymentRepository.findByOrganizationIdAndStatus(organizationId, PaymentStatus.PENDING);
    }

    public Payment getPaymentByBooking(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found for booking: " + bookingId));
    }

    // Balance sheet: total income minus total costs for the organization
    public BigDecimal calculateBalance(UUID organizationId) {
        return paymentRepository.findByOrganizationId(organizationId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
