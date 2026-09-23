package com.oracle.assistantapp.dto;

public record RecommendedProduct(Integer productId, String productName, String ingredient,
                                 Double requiredAmount, String requiredUnit, Integer quantity,
                                 Double unitPrice, Double lineTotal, String status) { }
