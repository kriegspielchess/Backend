package com.academy.dto;

public record AuthResponse(
        String token,
        UserDto user
) {
}
