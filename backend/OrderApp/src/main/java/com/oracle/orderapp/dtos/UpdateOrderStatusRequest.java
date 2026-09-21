package com.oracle.orderapp.dtos;

import com.oracle.orderapp.entities.OrderStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status,
        @Size(max = 1000) String cancellationReason
) {}
