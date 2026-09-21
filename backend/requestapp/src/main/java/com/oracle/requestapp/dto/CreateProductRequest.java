package com.oracle.requestapp.dto;

import com.oracle.requestapp.entities.RequestAction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotNull RequestAction action,
        Integer productId,
        @Size(max = 150) String name,
        @DecimalMin(value = "0.01", message = "price must be greater than zero") BigDecimal price,
        @Min(value = 1, message = "quantity must be at least one") Integer quantity,
        @Min(0) @Max(100) Integer discount,
        @Size(max = 2000) String description) {
}
