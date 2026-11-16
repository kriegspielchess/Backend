package com.academy.dto;

public record AdminPaymentSlip(
        byte[] data,
        String contentType,
        String fileName
) {
}
