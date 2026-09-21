package com.oracle.cartapp.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(
        @NotNull Integer productId,
        @NotNull @Positive Integer quantity
) {}
