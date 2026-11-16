package com.academy.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.academy.entity.PaymentStatus;

public record AdminPaymentDto(
        Long id,
        String studentName,
        String studentEmail,
        String guardianName,
        String whatsappNumber,
        String className,
        LocalDate paymentDate,
        String paymentSlipFileName,
        String paymentSlipContentType,
        PaymentStatus status,
        Instant submittedAt
) {
}
