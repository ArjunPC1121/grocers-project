package com.oracle.productsapp.dtos;

import com.oracle.productsapp.entities.ProductCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 100)
        String brand,

        @NotNull
        ProductCategory category,

        @Size(max = 100)
        String subCategory,

        @Size(max = 1000)
        String description,

        @Size(max = 1000)
        String tags,

        @Size(max = 1000)
        String searchAliases,

        @Positive
        Double unitValue,

        @Size(max = 20)
        String unitType,

        @Size(max = 1000)
        String imageUrl,

        @NotNull
        @Positive
        Double price,

        @Min(0)
        @Max(100)
        int discount,

        @NotNull
        @PositiveOrZero
        Integer quantity,

        Boolean active
) {}
