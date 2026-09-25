package com.oracle.assistantapp.services;

import com.oracle.assistantapp.dto.AssistantRequest;
import com.oracle.assistantapp.dto.IngredientPlan;
import com.oracle.assistantapp.dto.ProductCatalogItem;

import java.util.List;

public interface GeminiIngredientClient {
    IngredientPlan identifyIngredients(AssistantRequest request, List<ProductCatalogItem> catalogue);
}
