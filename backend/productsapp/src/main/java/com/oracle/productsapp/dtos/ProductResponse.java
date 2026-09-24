package com.oracle.productsapp.dtos;

import com.oracle.productsapp.entities.ProductCategory;

/** Public product fields, excluding search text and embedding metadata. */
public record ProductResponse(
        Integer id,
        String name,
        String brand,
        ProductCategory category,
        String subCategory,
        String description,
        String tags,
        String searchAliases,
        Double unitValue,
        String unitType,
        String imageUrl,
        Double price,
        Integer discount,
        Integer quantity,
        Boolean active
) { }
