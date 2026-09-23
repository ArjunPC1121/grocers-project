package com.oracle.employeeapp.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, message = "New password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[^\\w\\s]).{8,}$",
                message = "New password must include an uppercase letter and a symbol") String newPassword,
        @NotBlank String confirmPassword) { }
