package com.academy.dto;

import com.academy.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record PaymentStatusUpdateRequest(
        @NotNull PaymentStatus status
) {
}
