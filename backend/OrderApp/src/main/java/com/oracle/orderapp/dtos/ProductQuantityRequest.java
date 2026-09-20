package com.oracle.orderapp.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductQuantityRequest(
        @NotNull @Positive Integer quantity
) {}
