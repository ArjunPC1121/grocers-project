package com.oracle.employeeapp.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventoryRequest(@NotNull Integer productId, @NotNull InventoryOperation operation,
                               @NotNull @Positive Integer quantity, String description) { }
