package com.holidayplanner.paymentservice.controller;

import com.holidayplanner.paymentservice.model.Payment;
import com.holidayplanner.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("PaymentService is running!");
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @RequestParam UUID bookingId,
            @RequestParam UUID organizationId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(paymentService.createPayment(bookingId, organizationId, amount));
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<Payment>> getPaymentsByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrganization(organizationId));
    }

    @GetMapping("/organization/{organizationId}/pending")
    public ResponseEntity<List<Payment>> getPendingPayments(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(paymentService.getPendingPayments(organizationId));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<Payment> getPaymentByBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBooking(bookingId));
    }

    @PatchMapping("/{paymentId}/pay")
    public ResponseEntity<Payment> markAsPaid(
            @PathVariable UUID paymentId,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(paymentService.markAsPaid(paymentId, note));
    }

    @PatchMapping("/{paymentId}/refund")
    public ResponseEntity<Payment> refundPayment(
            @PathVariable UUID paymentId,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, note));
    }

    @GetMapping("/organization/{organizationId}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(paymentService.calculateBalance(organizationId));
    }
}
