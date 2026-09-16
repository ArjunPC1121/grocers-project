package com.oracle.orderapp.dtos;
import com.oracle.orderapp.entities.OrderStatus;
import jakarta.validation.constraints.NotNull;
public record OrderStatusUpdateRequest(@NotNull OrderStatus status) {}
