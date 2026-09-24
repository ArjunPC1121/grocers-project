package com.oracle.productsapp.dtos;

import java.math.BigDecimal;

/** Internal projection combining Oracle Text and database vector scores. */
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
        BigDecimal oracleTextScore,
        BigDecimal oracleSemanticScore
) {}
