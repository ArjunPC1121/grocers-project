package com.oracle.assistantapp.dto;

import java.math.BigDecimal;

public record RecommendedProduct(Integer productId, String productName, String ingredient,
                                 Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal) { }
