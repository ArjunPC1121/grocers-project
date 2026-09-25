package com.oracle.userapp.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

//Not in use
public record PaymentRequest(@NotNull @Positive Double amount,
                             @NotBlank String reference) {
}
