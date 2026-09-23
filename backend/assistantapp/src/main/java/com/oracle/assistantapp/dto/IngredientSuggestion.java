package com.oracle.assistantapp.dto;

public record IngredientSuggestion(String name, Double requiredAmount, String unit,
                                   Boolean mandatory, Integer productId) { }
