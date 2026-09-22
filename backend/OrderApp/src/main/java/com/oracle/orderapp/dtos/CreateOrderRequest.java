package com.oracle.orderapp.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull Integer customerId,
        Integer cartId,
        @NotBlank String deliveryAddress,
        @NotEmpty List<@Valid OrderItemRequest> items
) {}
