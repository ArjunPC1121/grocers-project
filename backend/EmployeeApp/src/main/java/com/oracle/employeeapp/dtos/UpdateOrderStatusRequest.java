package com.oracle.employeeapp.dtos;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrderStatusRequest(@NotBlank String status, String cancellationReason) { }
