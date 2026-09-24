package com.oracle.assistantapp.services;

import com.oracle.assistantapp.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroceryAssistantServiceTests {
    @Test
    void calculatesMinimumPackagesAndDiscountedPriceWithoutTrustingAProductId() {
        GeminiIngredientClient gemini = (request, catalogue) -> plan("paneer butter masala", ingredient("paneer", List.of("cottage cheese"), 1.0, IngredientUnit.KG, IngredientRequirement.REQUIRED, IngredientRole.DAIRY));
        ProductCatalogClient catalogue = () -> List.of(product(1, "Fresh Paneer", 400.0, "g", 100.0, 10, 3, true));

        RecommendationResponse response = service(gemini, catalogue).recommend(new AssistantRequest("Paneer butter masala", 700.0, 3));

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
    void omitsUnavailableOptionalIngredientsAndUsesOneLargerPack() {
        GeminiIngredientClient gemini = (request, catalogue) -> plan("paneer curry",
                ingredient("paneer", List.of("cottage cheese"), 500.0, IngredientUnit.G, IngredientRequirement.REQUIRED, IngredientRole.DAIRY),
                ingredient("coriander", List.of("cilantro"), 10.0, IngredientUnit.G, IngredientRequirement.OPTIONAL, IngredientRole.GARNISH));
        ProductCatalogClient catalogue = () -> List.of(product(1, "Fresh Paneer", 1.0, "kg", 120.0, 0, 1, true));

        RecommendationResponse response = service(gemini, catalogue).recommend(new AssistantRequest("Paneer curry", null, 2));

        assertEquals(1, response.recommendedProducts().size());
        assertEquals(1, response.recommendedProducts().get(0).quantity());
        assertNull(response.budget());
        assertNull(response.withinBudget());
    }

    @Test
    void returnsOnlyFriedRiceRequirementsRatherThanOtherCatalogueProducts() {
        GeminiIngredientClient gemini = (request, catalogue) -> plan("fried rice",
                ingredient("rice", List.of("basmati rice", "long grain rice"), 500.0, IngredientUnit.G, IngredientRequirement.REQUIRED, IngredientRole.BASE),
                ingredient("mixed vegetables", List.of("vegetable mix"), 300.0, IngredientUnit.G, IngredientRequirement.REQUIRED, IngredientRole.VEGETABLE),
                ingredient("cooking oil", List.of("sunflower oil", "vegetable oil"), 30.0, IngredientUnit.ML, IngredientRequirement.REQUIRED, IngredientRole.FAT));
        ProductCatalogClient catalogue = () -> List.of(
                product(1, "Basmati Rice", 1.0, "kg", 90.0, 0, 3, true),
                product(2, "Mixed Vegetable Pack", 500.0, "g", 50.0, 0, 3, true),
                product(3, "Sunflower Oil", 1.0, "l", 140.0, 0, 3, true),
                product(4, "Fresh Paneer", 200.0, "g", 75.0, 0, 3, true),
                product(5, "Chicken Curry Cut", 1.0, "kg", 250.0, 0, 3, true));

        RecommendationResponse response = service(gemini, catalogue).recommend(new AssistantRequest("fried rice", null, 2));

        assertEquals(List.of("rice", "mixed vegetables", "cooking oil"), response.recommendedProducts().stream().map(RecommendedProduct::ingredient).toList());
        assertFalse(response.recommendedProducts().stream().anyMatch(product -> "Fresh Paneer".equals(product.productName()) || "Chicken Curry Cut".equals(product.productName())));
    }

    @Test
    void marksRequiredIngredientOutOfStockWhenAvailablePacksAreInsufficient() {
        GeminiIngredientClient gemini = (request, catalogue) -> plan("paneer curry", ingredient("paneer", List.of("cottage cheese"), 1.0, IngredientUnit.KG, IngredientRequirement.REQUIRED, IngredientRole.DAIRY));
        ProductCatalogClient catalogue = () -> List.of(product(1, "Fresh Paneer", 400.0, "g", 100.0, 0, 2, true));

        RecommendedProduct recommendation = service(gemini, catalogue).recommend(new AssistantRequest("Paneer curry", null, 2)).recommendedProducts().get(0);

        assertEquals(3, recommendation.quantity());
        assertEquals("OUT_OF_STOCK", recommendation.status());
        assertEquals("INSUFFICIENT_STOCK", recommendation.availabilityReason());
        assertNull(recommendation.lineTotal());
    }

    private GroceryAssistantService service(GeminiIngredientClient gemini, ProductCatalogClient catalogue) {
        return new GroceryAssistantService(gemini, catalogue, new CatalogueIngredientMatcher());
    }

    private IngredientPlan plan(String dish, IngredientSuggestion... ingredients) {
        return new IngredientPlan(dish, "Dinner", List.of(ingredients));
    }

    private IngredientSuggestion ingredient(String name, List<String> terms, Double amount, IngredientUnit unit,
                                           IngredientRequirement requirement, IngredientRole role) {
        return new IngredientSuggestion(name, terms, amount, unit, requirement, role);
    }

    private ProductCatalogItem product(Integer id, String name, Double unitValue, String unitType, Double price,
                                       Integer discount, Integer quantity, Boolean active) {
        return new ProductCatalogItem(id, name, null, null, null, null, null, name.toLowerCase(), unitValue,
                unitType, null, price, discount, quantity, active);
    }
}
