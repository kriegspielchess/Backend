package com.academy.dto;

import com.academy.entity.Role;
import com.academy.entity.StudentProfileStatus;

public record UserDto(
        Long id,
        String name,
        String email,
        Role role,
        Integer age,
        String className,
        String teacherName,
        String whatsappNumber,
        String guardianName,
        Boolean rated,
        String fideId,
        String requestedClassName,
        String requestedTeacherName,
        StudentProfileStatus profileStatus
) {
}
