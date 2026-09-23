package com.oracle.orderapp.dtos;

import com.oracle.orderapp.entities.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull Integer customerId,
        Integer cartId,
        @NotBlank String deliveryAddress,
        @NotNull PaymentMethod paymentMethod,
        @NotEmpty List<@Valid OrderItemRequest> items
) {}
