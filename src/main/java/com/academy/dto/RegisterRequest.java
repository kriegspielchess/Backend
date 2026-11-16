package com.academy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotNull @Min(1) @Max(120) Integer age,
        @NotBlank String className,
        @NotBlank String teacherName,
        @NotBlank String whatsappNumber,
        @NotBlank String guardianName,
        @NotNull Boolean rated,
        String fideId,
        @NotBlank String studentPhoto
) {
}
