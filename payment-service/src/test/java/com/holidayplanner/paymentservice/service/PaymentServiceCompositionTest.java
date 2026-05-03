package com.holidayplanner.paymentservice.service;

import com.holidayplanner.paymentservice.client.BookingServiceClient;
import com.holidayplanner.paymentservice.client.EventServiceClient;
import com.holidayplanner.paymentservice.dto.BookingClientResponse;
import com.holidayplanner.paymentservice.dto.EventTermClientResponse;
import com.holidayplanner.paymentservice.dto.EventTermPaymentOverviewResponse;
import com.holidayplanner.paymentservice.kafka.PaymentEventProducer;
import com.holidayplanner.paymentservice.repository.PaymentRepository;
import com.holidayplanner.shared.model.BookingStatus;
import com.holidayplanner.shared.model.EventTermStatus;
import com.holidayplanner.shared.model.Payment;
import com.holidayplanner.shared.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceCompositionTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private BookingServiceClient bookingServiceClient;

    @Mock
    private EventServiceClient eventServiceClient;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void getEventTermPaymentOverviewComposesEventBookingsAndPayments() {
        UUID eventTermId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID eventId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID bookingIdPaid = UUID.fromString("66666666-6666-6666-6666-666666666666");
        UUID bookingIdMissingPayment = UUID.fromString("77777777-7777-7777-7777-777777777777");
        UUID familyMemberId1 = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID familyMemberId2 = UUID.fromString("55555555-5555-5555-5555-555555555555");

        EventTermClientResponse eventTerm = new EventTermClientResponse();
        eventTerm.setId(eventTermId);
        eventTerm.setEventId(eventId);
        eventTerm.setEventName("Bicycle Tour");
        eventTerm.setEventLocation("Linz");
        eventTerm.setPrice(new BigDecimal("30.00"));
        eventTerm.setStartDateTime(LocalDateTime.of(2026, 7, 12, 9, 0));
        eventTerm.setEndDateTime(LocalDateTime.of(2026, 7, 12, 14, 0));
        eventTerm.setMinParticipants(5);
        eventTerm.setMaxParticipants(20);
        eventTerm.setStatus(EventTermStatus.ACTIVE);

        BookingClientResponse paidBooking = new BookingClientResponse();
        paidBooking.setId(bookingIdPaid);
        paidBooking.setFamilyMemberId(familyMemberId1);
        paidBooking.setEventTermId(eventTermId);
        paidBooking.setStatus(BookingStatus.CONFIRMED);
        paidBooking.setBookedAt(LocalDateTime.of(2026, 6, 1, 10, 0));

        BookingClientResponse missingPaymentBooking = new BookingClientResponse();
        missingPaymentBooking.setId(bookingIdMissingPayment);
        missingPaymentBooking.setFamilyMemberId(familyMemberId2);
        missingPaymentBooking.setEventTermId(eventTermId);
        missingPaymentBooking.setStatus(BookingStatus.CONFIRMED);
        missingPaymentBooking.setBookedAt(LocalDateTime.of(2026, 6, 2, 10, 0));

        Payment paidPayment = new Payment();
        paidPayment.setId(UUID.fromString("88888888-8888-8888-8888-888888888888"));
        paidPayment.setBookingId(bookingIdPaid);
        paidPayment.setOrganizationId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        paidPayment.setAmount(new BigDecimal("30.00"));
        paidPayment.setStatus(PaymentStatus.PAID);
        paidPayment.setPaidAt(LocalDateTime.of(2026, 6, 3, 12, 0));

        when(eventServiceClient.getEventTerm(eventTermId)).thenReturn(eventTerm);
        when(bookingServiceClient.getBookingsForEventTerm(eventTermId))
                .thenReturn(List.of(paidBooking, missingPaymentBooking));
        when(paymentRepository.findByBookingIdIn(List.of(bookingIdPaid, bookingIdMissingPayment)))
                .thenReturn(List.of(paidPayment));

        EventTermPaymentOverviewResponse response =
                paymentService.getEventTermPaymentOverview(eventTermId);

        assertEquals(eventTermId, response.getEventTermId());
        assertEquals("Bicycle Tour", response.getEventName());
        assertEquals(2, response.getBookingCount());
        assertEquals(2, response.getBillableBookingCount());

        assertEquals(1, response.getPaidCount());
        assertEquals(0, response.getPendingCount());
        assertEquals(0, response.getRefundedCount());
        assertEquals(1, response.getMissingPaymentCount());

        assertEquals(new BigDecimal("60.00"), response.getTotalExpectedAmount());
        assertEquals(new BigDecimal("30.00"), response.getTotalPaidAmount());
        assertEquals(new BigDecimal("0"), response.getTotalPendingAmount());
        assertEquals(new BigDecimal("30.00"), response.getTotalOpenAmount());

        assertEquals(2, response.getParticipants().size());
        assertEquals(true, response.getParticipants().get(0).isPaymentAvailable());
        assertEquals(false, response.getParticipants().get(1).isPaymentAvailable());
    }
}