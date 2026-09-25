package com.oracle.orderapp.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Employee-only cancellation request used to claim an order before cancelling it. */
public record EmployeeCancelOrderRequest(
        @NotBlank String reason,
        @NotNull Integer employeeId
) { }
