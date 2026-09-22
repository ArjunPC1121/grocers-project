package com.oracle.authapp.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String role,
        boolean mustChangePassword,
        String message
) { }
