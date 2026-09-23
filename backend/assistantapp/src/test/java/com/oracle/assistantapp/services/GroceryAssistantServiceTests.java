package com.oracle.assistantapp.services;

import com.oracle.assistantapp.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroceryAssistantServiceTests {
    @Test
    void calculatesMinimumPackagesAndDiscountedPrice() {
        GeminiIngredientClient gemini = (request, catalogue) -> new IngredientPlan("paneer butter masala", "Dinner for three",
                List.of(new IngredientSuggestion("paneer", 1.0, "kg", true, 1)));
        ProductCatalogClient catalogue = () -> List.of(
                product(1, "Fresh Paneer", 400.0, "g", 100.0, 10, 3, true));

        RecommendationResponse response = new GroceryAssistantService(gemini, catalogue)
                .recommend(new AssistantRequest("Paneer butter masala", 700.0, 3));

        RecommendedProduct recommendation = response.recommendedProducts().get(0);
        assertEquals(1, recommendation.productId());
        assertEquals(3, recommendation.quantity());
        assertEquals(90.0, recommendation.unitPrice());
        assertEquals(270.0, recommendation.lineTotal());
        assertEquals("IN_STOCK", recommendation.status());
        assertEquals(270.0, response.total());
        assertTrue(response.withinBudget());
    }

    @Test
    void usesOneLargerPackAndOmitsMissingOptionalIngredients() {
        GeminiIngredientClient gemini = (request, catalogue) -> new IngredientPlan("paneer curry", "Dinner",
                List.of(new IngredientSuggestion("paneer", 500.0, "g", true, 1),
                        new IngredientSuggestion("coriander", 10.0, "g", false, null)));
        ProductCatalogClient catalogue = () -> List.of(product(1, "Fresh Paneer", 1.0, "kg", 120.0, 0, 1, true));

        RecommendationResponse response = new GroceryAssistantService(gemini, catalogue)
                .recommend(new AssistantRequest("Paneer curry", null, 2));

        assertEquals(1, response.recommendedProducts().size());
        assertEquals(1, response.recommendedProducts().get(0).quantity());
        assertNull(response.budget());
        assertNull(response.withinBudget());
    }

    @Test
    void marksEssentialIngredientOutOfStockWhenPackagesExceedInventory() {
        GeminiIngredientClient gemini = (request, catalogue) -> new IngredientPlan("paneer curry", "Dinner",
                List.of(new IngredientSuggestion("paneer", 1.0, "kg", true, 1)));
        ProductCatalogClient catalogue = () -> List.of(product(1, "Fresh Paneer", 400.0, "g", 100.0, 0, 2, true));

        RecommendationResponse response = new GroceryAssistantService(gemini, catalogue)
                .recommend(new AssistantRequest("Paneer curry", null, 2));

        RecommendedProduct recommendation = response.recommendedProducts().get(0);
        assertEquals(3, recommendation.quantity());
        assertEquals("OUT_OF_STOCK", recommendation.status());
        assertNull(recommendation.lineTotal());
        assertEquals(0.0, response.total());
    }

    private ProductCatalogItem product(Integer id, String name, Double unitValue, String unitType,
                                       Double price, Integer discount, Integer quantity, Boolean active) {
        return new ProductCatalogItem(id, name, null, null, null, null, null, name.toLowerCase(), unitValue,
                unitType, null, price, discount, quantity, active);
    }
}
