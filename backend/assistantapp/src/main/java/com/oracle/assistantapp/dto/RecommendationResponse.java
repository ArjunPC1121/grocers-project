package com.oracle.assistantapp.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationResponse(String dish, String summary, List<RecommendedProduct> availableProducts,
                                     List<String> missingIngredients, BigDecimal total, BigDecimal budget,
                                     boolean withinBudget) { }
