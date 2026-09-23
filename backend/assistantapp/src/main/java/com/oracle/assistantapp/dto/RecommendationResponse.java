package com.oracle.assistantapp.dto;

import java.util.List;

public record RecommendationResponse(String dish, String summary, List<RecommendedProduct> recommendedProducts,
                                     Double total, Double budget, Boolean withinBudget) { }
