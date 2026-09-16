package com.oracle.authapp.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String role
) { }
