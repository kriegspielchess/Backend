package com.academy.dto;

import java.time.Instant;

import com.academy.entity.StudentProfileStatus;

public record AdminStudentReviewDto(
        Long id,
        String name,
        String email,
        Integer age,
        String requestedClassName,
        String requestedTeacherName,
        String whatsappNumber,
        String guardianName,
        Boolean rated,
        String fideId,
        String studentPhotoContentType,
        StudentProfileStatus profileStatus,
        Instant createdAt
) {
}
