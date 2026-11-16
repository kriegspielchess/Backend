package com.academy.dto;

public record AdminStudentPhoto(
        byte[] data,
        String contentType,
        String fileName
) {
}
