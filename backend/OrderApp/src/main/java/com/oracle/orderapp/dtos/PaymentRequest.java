package com.oracle.orderapp.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String reference
) {}
