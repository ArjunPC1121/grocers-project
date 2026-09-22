package com.oracle.adminapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        LocalDate dob,
        @NotBlank String phoneNumber,
        @NotBlank String address,
        @NotBlank String accountNumber,
        @NotBlank String secretQuestion,
        @NotBlank String secretAnswer) {
}
