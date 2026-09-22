package com.oracle.assistantapp.services;

import com.oracle.assistantapp.dto.AssistantRequest;
import com.oracle.assistantapp.dto.IngredientPlan;

public interface GeminiIngredientClient {
    IngredientPlan identifyIngredients(AssistantRequest request);
}
