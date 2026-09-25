package com.oracle.userapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Carries the new password requested by an authenticated user.
public record ChangePasswordRequest(
        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {
}
