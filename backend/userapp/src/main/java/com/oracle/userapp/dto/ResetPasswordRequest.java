package com.oracle.userapp.dto;

import jakarta.validation.constraints.NotBlank;

// Carries a recovery token and the password it should set.
public record ResetPasswordRequest(
        @NotBlank String resetToken,
        @NotBlank String newPassword
) {
}
