package com.academy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.dto.AdminPaymentDto;
import com.academy.dto.AdminPaymentSlip;
import com.academy.entity.PaymentStatus;
import com.academy.entity.StudentPayment;
import com.academy.exception.ResourceNotFoundException;
import com.academy.repository.StudentPaymentRepository;

@ExtendWith(MockitoExtension.class)
class AdminPaymentServiceTest {

    @Mock
    private StudentPaymentRepository paymentRepository;

    @InjectMocks
    private AdminPaymentService service;

    @Test
    void findAllDelegatesToRepository() {
        List<AdminPaymentDto> payments = List.of();
        when(paymentRepository.findAdminDtos()).thenReturn(payments);

        assertSame(payments, service.findAll());
    }

    @Test
    void updatesPendingPaymentToCheckedOrVerified() {
        StudentPayment checked = payment(PaymentStatus.PENDING_REVIEW);
        StudentPayment verified = payment(PaymentStatus.CHECKED);
        StudentPayment alreadyVerified = payment(PaymentStatus.VERIFIED);
        AdminPaymentDto checkedDto = dto(PaymentStatus.CHECKED);
        AdminPaymentDto verifiedDto = dto(PaymentStatus.VERIFIED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(checked));
        when(paymentRepository.findById(2L)).thenReturn(Optional.of(verified));
        when(paymentRepository.findById(3L)).thenReturn(Optional.of(alreadyVerified));
        when(paymentRepository.findAdminDtoById(1L)).thenReturn(Optional.of(checkedDto));
        when(paymentRepository.findAdminDtoById(2L)).thenReturn(Optional.of(verifiedDto));
        when(paymentRepository.findAdminDtoById(3L)).thenReturn(Optional.of(verifiedDto));

        assertSame(checkedDto, service.updateStatus(1L, PaymentStatus.CHECKED));
        assertSame(verifiedDto, service.updateStatus(2L, PaymentStatus.VERIFIED));
        assertSame(verifiedDto, service.updateStatus(3L, PaymentStatus.VERIFIED));
        assertEquals(PaymentStatus.CHECKED, checked.getStatus());
        assertEquals(PaymentStatus.VERIFIED, verified.getStatus());
        assertEquals(PaymentStatus.VERIFIED, alreadyVerified.getStatus());
    }

    @Test
    void rejectsUnsupportedStatusesBeforeReadingPayment() {
        for (PaymentStatus status : List.of(PaymentStatus.PENDING_REVIEW, PaymentStatus.APPROVED, PaymentStatus.REJECTED)) {
            assertThrows(IllegalArgumentException.class, () -> service.updateStatus(1L, status));
        }
    }

    @Test
    void rejectsMissingOrTerminalPaymentsAndMissingProjection() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());
        when(paymentRepository.findById(2L)).thenReturn(Optional.of(payment(PaymentStatus.APPROVED)));
        when(paymentRepository.findById(3L)).thenReturn(Optional.of(payment(PaymentStatus.REJECTED)));
        when(paymentRepository.findById(4L)).thenReturn(Optional.of(payment(PaymentStatus.VERIFIED)));
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment(PaymentStatus.PENDING_REVIEW)));
        when(paymentRepository.findAdminDtoById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateStatus(1L, PaymentStatus.CHECKED));
        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(2L, PaymentStatus.CHECKED));
        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(3L, PaymentStatus.VERIFIED));
        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(4L, PaymentStatus.CHECKED));
        assertThrows(ResourceNotFoundException.class, () -> service.updateStatus(5L, PaymentStatus.VERIFIED));
    }

    @Test
    void getsSlipAndReportsMissingPayment() {
        byte[] data = {1, 2, 3};
        StudentPayment payment = StudentPayment.builder().paymentSlip(data)
                .paymentSlipContentType("image/png").paymentSlipFileName("receipt.png").build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.findById(2L)).thenReturn(Optional.empty());

        AdminPaymentSlip slip = service.getSlip(1L);

        assertSame(data, slip.data());
        assertEquals("image/png", slip.contentType());
        assertEquals("receipt.png", slip.fileName());
        assertThrows(ResourceNotFoundException.class, () -> service.getSlip(2L));
        verify(paymentRepository).findById(1L);
    }

    private StudentPayment payment(PaymentStatus status) {
        return StudentPayment.builder().status(status).build();
    }

    private AdminPaymentDto dto(PaymentStatus status) {
        return new AdminPaymentDto(1L, "Student", "student@example.com", null, null, "Beginner",
                null, "receipt.png", "image/png", status, null);
    }
}
