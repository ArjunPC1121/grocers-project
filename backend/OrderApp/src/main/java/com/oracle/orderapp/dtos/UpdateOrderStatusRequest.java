package com.oracle.orderapp.dtos;

import com.oracle.orderapp.entities.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull Integer employeeId,
        @NotNull OrderStatus status
) {}