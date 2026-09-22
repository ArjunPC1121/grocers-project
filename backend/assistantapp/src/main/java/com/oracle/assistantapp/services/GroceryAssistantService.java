package com.oracle.assistantapp.services;

import com.oracle.assistantapp.dto.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class GroceryAssistantService {
    private final GeminiIngredientClient geminiIngredientClient;
    private final ProductCatalogClient productCatalogClient;

    public GroceryAssistantService(GeminiIngredientClient geminiIngredientClient, ProductCatalogClient productCatalogClient) {
        this.geminiIngredientClient = geminiIngredientClient;
        this.productCatalogClient = productCatalogClient;
    }

    public RecommendationResponse recommend(AssistantRequest request) {
        IngredientPlan plan = geminiIngredientClient.identifyIngredients(request);
        List<ProductCatalogItem> catalogue = productCatalogClient.getProducts();
        List<RecommendedProduct> available = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (IngredientSuggestion ingredient : plan.ingredients()) {
            if (ingredient == null || ingredient.name() == null || ingredient.name().isBlank()) continue;
            int requestedQuantity = Math.max(1, Math.min(10, ingredient.quantity() == null ? 1 : ingredient.quantity()));
            ProductCatalogItem product = findProduct(ingredient.name(), catalogue, requestedQuantity);
            if (product == null) {
                missing.add(ingredient.name());
                continue;
            }
            BigDecimal unitPrice = discountedPrice(product);
            available.add(new RecommendedProduct(product.id(), product.name(), ingredient.name(), requestedQuantity,
                    unitPrice, unitPrice.multiply(BigDecimal.valueOf(requestedQuantity)).setScale(2, RoundingMode.HALF_UP)));
        }
        BigDecimal total = available.stream().map(RecommendedProduct::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new RecommendationResponse(plan.dish(), plan.summary(), available, missing, total, request.budget(),
                total.compareTo(request.budget()) <= 0);
    }

    private ProductCatalogItem findProduct(String ingredient, List<ProductCatalogItem> catalogue, int requestedQuantity) {
        String normalizedIngredient = normalize(ingredient);
        return catalogue.stream()
                .filter(product -> product.id() != null && product.name() != null && product.price() != null
                        && product.quantity() != null && product.quantity() >= requestedQuantity)
                .filter(product -> matches(normalizedIngredient, normalize(product.name())))
                .min(Comparator.comparing(this::discountedPrice))
                .orElse(null);
    }

    private boolean matches(String ingredient, String product) {
        return product.equals(ingredient) || product.contains(ingredient) || ingredient.contains(product);
    }

    private BigDecimal discountedPrice(ProductCatalogItem product) {
        int discount = product.discount() == null ? 0 : Math.max(0, Math.min(100, product.discount()));
        return product.price().multiply(BigDecimal.valueOf(100 - discount))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
