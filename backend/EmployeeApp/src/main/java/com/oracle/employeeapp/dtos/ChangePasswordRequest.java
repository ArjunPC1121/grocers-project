package com.oracle.employeeapp.dtos;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(@NotBlank String newPassword, @NotBlank String confirmPassword) { }
