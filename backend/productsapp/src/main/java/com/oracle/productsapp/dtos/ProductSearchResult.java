package com.oracle.productsapp.dtos;

import java.math.BigDecimal;

public record ProductSearchResult(
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
        BigDecimal lexicalScore,
        BigDecimal semanticScore,
        BigDecimal finalScore
) {}