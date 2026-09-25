package com.oracle.productsapp.dtos;

import java.math.BigDecimal;

/** Internal projection used for application-side semantic ranking. */
public record ProductSearchCandidate(
        Integer id,
        String name,
        String brand,
        String category,
        String subCategory,
        String description,
        String imageUrl,
        Double price,
        Integer discount,
        Integer quantity,
        String searchAliases,
        String tags,
        float[] embedding,
        BigDecimal oracleTextScore
) {}
