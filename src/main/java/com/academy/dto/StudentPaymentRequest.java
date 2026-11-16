package com.academy.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentPaymentRequest(
        @NotNull Long courseId,
        @NotNull LocalDate paymentDate,
        @NotBlank String paymentSlipDataUrl,
        @NotBlank @Size(max = 255) String paymentSlipFileName
) {
}
