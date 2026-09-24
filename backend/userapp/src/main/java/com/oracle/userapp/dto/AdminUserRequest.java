package com.oracle.userapp.dto;

import com.oracle.userapp.entities.SecretQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.sql.Date;

//Not in use
public record AdminUserRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String email,
        @NotNull Date dob,
        @NotBlank String phoneNumber,
        @NotBlank String address,
        @NotBlank String accountNumber,
        @NotNull SecretQuestion secretQuestion,
        @NotBlank String secretAnswer) {
}
