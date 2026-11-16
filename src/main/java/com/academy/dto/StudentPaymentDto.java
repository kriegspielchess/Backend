package com.academy.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.academy.entity.PaymentStatus;

public record StudentPaymentDto(
        Long id,
        String className,
        LocalDate paymentDate,
        String paymentSlipFileName,
        PaymentStatus status,
        Instant submittedAt
) {
}
