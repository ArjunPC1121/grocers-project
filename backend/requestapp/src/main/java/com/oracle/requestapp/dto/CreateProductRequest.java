/**
 * Component role: Defines a transport contract used at an API or service boundary. Keep it free of persistence and business side effects.
 *
 * Maintainer note: this file belongs to requestapp. See backend/requestapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.requestapp.dto;

import com.oracle.requestapp.entities.RequestAction;
import com.oracle.requestapp.entities.ProductCategory;
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
        @Size(max = 100) String brand,
        ProductCategory category,
        @Size(max = 100) String subCategory,
        @DecimalMin(value = "0.01", message = "price must be greater than zero") BigDecimal price,
        @Min(value = 1, message = "quantity must be at least one") Integer quantity,
        @Min(0) @Max(100) Integer discount,
        @Size(max = 2000) String description,
        @Size(max = 1000) String tags,
        @Size(max = 1000) String searchAliases,
        Double unitValue,
        @Size(max = 20) String unitType,
        Boolean active,
        @Size(max = 1000) String imageUrl,
        @Size(max = 255) String imageFileName,
        @Size(max = 2000) String reason,
        @Size(max = 4000) String previousValues) {
}
