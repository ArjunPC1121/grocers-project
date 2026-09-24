package com.oracle.assistantapp.dto;

import java.util.List;

/** A recipe requirement. Catalogue selection is performed inside Assistant App. */
public record IngredientSuggestion(String canonicalName, List<String> matchTerms,
                                   Double requiredAmount, IngredientUnit unit,
                                   IngredientRequirement requirement, IngredientRole role) { }
