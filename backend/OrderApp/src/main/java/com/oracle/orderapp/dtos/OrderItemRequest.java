package com.oracle.orderapp.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotNull @Positive Integer productId,
        @NotNull @Positive Integer quantity
) {}
