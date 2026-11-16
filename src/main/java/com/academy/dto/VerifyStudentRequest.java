package com.academy.dto;

import jakarta.validation.constraints.NotNull;

public record VerifyStudentRequest(
        @NotNull Long courseId
) {
}
