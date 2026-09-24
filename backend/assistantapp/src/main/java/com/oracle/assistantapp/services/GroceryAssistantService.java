package com.oracle.assistantapp.services;

import com.oracle.assistantapp.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class GroceryAssistantService {
    private final GeminiIngredientClient geminiIngredientClient;
    private final ProductCatalogClient productCatalogClient;
    private final CatalogueIngredientMatcher catalogueIngredientMatcher;

    @Autowired
    public GroceryAssistantService(GeminiIngredientClient geminiIngredientClient, ProductCatalogClient productCatalogClient,
                                  CatalogueIngredientMatcher catalogueIngredientMatcher) {
        this.geminiIngredientClient = geminiIngredientClient;
        this.productCatalogClient = productCatalogClient;
        this.catalogueIngredientMatcher = catalogueIngredientMatcher;
    }

    GroceryAssistantService(GeminiIngredientClient geminiIngredientClient, ProductCatalogClient productCatalogClient) {
        this(geminiIngredientClient, productCatalogClient, new CatalogueIngredientMatcher());
    }

    public RecommendationResponse recommend(AssistantRequest request) {
        List<ProductCatalogItem> catalogue = productCatalogClient.getProducts();
        IngredientPlan plan = geminiIngredientClient.identifyIngredients(request, catalogue);
        List<RecommendedProduct> recommendations = new ArrayList<>();
        for (IngredientSuggestion ingredient : plan.ingredients()) {
            if (!isValidIngredient(ingredient)) continue;
            CatalogueIngredientMatcher.MatchResult match = catalogueIngredientMatcher.findBestMatch(ingredient, catalogue);
            ProductCatalogItem product = match.product();
            if (product == null) {
                addUnavailableIfRequired(recommendations, ingredient, null, null, match.reason());
                continue;
            }
            Integer packagesRequired = calculatePackagesRequired(ingredient, product);
            if (packagesRequired == null) {
                addUnavailableIfRequired(recommendations, ingredient, product, null, "INCOMPATIBLE_UNIT");
            } else if (!Boolean.TRUE.equals(product.active())) {
                addUnavailableIfRequired(recommendations, ingredient, product, packagesRequired, "INACTIVE_PRODUCT");
            } else if (product.quantity() == null || product.quantity() < packagesRequired) {
                addUnavailableIfRequired(recommendations, ingredient, product, packagesRequired, "INSUFFICIENT_STOCK");
            } else {
                Double unitPrice = discountedPrice(product);
                recommendations.add(new RecommendedProduct(product.id(), product.name(), ingredient.canonicalName(),
                        ingredient.requiredAmount(), ingredient.unit().label(), packagesRequired, unitPrice,
                        roundMoney(unitPrice * packagesRequired), "IN_STOCK", "IN_STOCK"));
            }
        }
        // List.sort is stable, so recipe order is retained within each availability group.
        recommendations.sort(Comparator.comparing(product -> "IN_STOCK".equals(product.status()) ? 0 : 1));
        Double total = roundMoney(recommendations.stream().filter(product -> "IN_STOCK".equals(product.status()))
                .map(RecommendedProduct::lineTotal).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum());
        Boolean withinBudget = request.budget() == null ? null : total <= request.budget();
        return new RecommendationResponse(plan.dish(), plan.summary(), recommendations, total, request.budget(), withinBudget);
    }

    private void addUnavailableIfRequired(List<RecommendedProduct> recommendations, IngredientSuggestion ingredient,
                                          ProductCatalogItem product, Integer packagesRequired, String reason) {
        if (ingredient.requirement() == IngredientRequirement.OPTIONAL) return;
        recommendations.add(new RecommendedProduct(product == null ? null : product.id(), product == null ? null : product.name(),
                ingredient.canonicalName(), ingredient.requiredAmount(), ingredient.unit().label(), packagesRequired,
                null, null, "OUT_OF_STOCK", reason));
    }

    private boolean isValidIngredient(IngredientSuggestion ingredient) {
        return ingredient != null && ingredient.canonicalName() != null && !ingredient.canonicalName().isBlank()
                && ingredient.requiredAmount() != null && ingredient.requiredAmount() > 0 && ingredient.unit() != null
                && ingredient.requirement() != null && ingredient.role() != null;
    }

    private Integer calculatePackagesRequired(IngredientSuggestion ingredient, ProductCatalogItem product) {
        if (product.unitValue() == null || product.unitValue() <= 0) return null;
        String packageUnit = normalizeUnit(product.unitType());
        if (!sameUnitFamily(ingredient.unit().label(), packageUnit)) return null;
        Double requiredBase = toBaseUnit(ingredient.requiredAmount(), ingredient.unit().label());
        Double packageBase = toBaseUnit(product.unitValue(), packageUnit);
        if (requiredBase == null || packageBase == null) return null;
        return (int) Math.ceil(requiredBase / packageBase);
    }

    private String normalizeUnit(String unit) {
        if (unit == null) return "";
        return switch (unit.trim().toLowerCase(Locale.ROOT)) {
            case "g", "gm", "gram", "grams" -> "g";
            case "kg", "kgs", "kilogram", "kilograms" -> "kg";
            case "ml", "millilitre", "millilitres", "milliliter", "milliliters" -> "ml";
            case "l", "lt", "litre", "litres", "liter", "liters" -> "l";
            case "unit", "units", "piece", "pieces", "pc", "pcs", "count" -> "unit";
            default -> "";
        };
    }

    private Double toBaseUnit(Double amount, String unit) {
        if (amount == null || amount <= 0) return null;
        return switch (unit) {
            case "kg", "l" -> amount * 1000;
            case "g", "ml", "unit" -> amount;
            default -> null;
        };
    }

    private boolean sameUnitFamily(String first, String second) {
        return (isWeight(first) && isWeight(second)) || (isVolume(first) && isVolume(second))
                || ("unit".equals(first) && "unit".equals(second));
    }
    private boolean isWeight(String unit) { return "g".equals(unit) || "kg".equals(unit); }
    private boolean isVolume(String unit) { return "ml".equals(unit) || "l".equals(unit); }
    private Double discountedPrice(ProductCatalogItem product) {
        int discount = product.discount() == null ? 0 : Math.max(0, Math.min(100, product.discount()));
        return roundMoney(product.price() * (100 - discount) / 100);
    }
    private Double roundMoney(Double amount) { return Math.round(amount * 100.0) / 100.0; }
}
