package com.oracle.adminapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/** Password changes stay in UserApp and are deliberately not exposed through AdminApp. */
public record UserUpdateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        LocalDate dob,
        @NotBlank String phoneNumber,
        @NotBlank String address,
        @NotBlank String accountNumber) {
}
