package com.oracle.authapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SecurityRecoveryResetPasswordRequest(@NotBlank @Email String email, @NotBlank String resetToken,
                                                   @NotBlank @Size(min = 8, max = 100) String newPassword) { }
