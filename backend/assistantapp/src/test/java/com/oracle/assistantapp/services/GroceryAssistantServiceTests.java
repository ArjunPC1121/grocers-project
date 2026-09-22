package com.oracle.assistantapp.services;

import com.oracle.assistantapp.dto.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroceryAssistantServiceTests {
    @Test
    void usesOnlyInStockCatalogueProductsAndCalculatesDiscountedPrice() {
        GeminiIngredientClient gemini = request -> new IngredientPlan("paneer butter masala", "Dinner for three",
                List.of(new IngredientSuggestion("paneer", 2), new IngredientSuggestion("cream", 1)));
        ProductCatalogClient catalogue = () -> List.of(
                new ProductCatalogItem(1, "Fresh Paneer", new BigDecimal("280"), 10, 5),
                new ProductCatalogItem(2, "Cooking Cream", new BigDecimal("100"), 0, 0));

        RecommendationResponse response = new GroceryAssistantService(gemini, catalogue)
                .recommend(new AssistantRequest("Paneer butter masala", new BigDecimal("700"), 3));

        assertEquals(1, response.availableProducts().size());
        assertEquals(1, response.availableProducts().get(0).productId());
        assertEquals(new BigDecimal("252.00"), response.availableProducts().get(0).unitPrice());
        assertEquals(new BigDecimal("504.00"), response.total());
        assertEquals(List.of("cream"), response.missingIngredients());
        assertTrue(response.withinBudget());
    }
}
