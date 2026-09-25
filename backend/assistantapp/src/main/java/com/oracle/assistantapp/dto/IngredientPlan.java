package com.oracle.assistantapp.dto;

import java.util.List;

public record IngredientPlan(String dish, String summary, List<IngredientSuggestion> ingredients) { }
