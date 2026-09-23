package com.oracle.assistantapp.services;

import com.oracle.assistantapp.dto.*;
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

    public GroceryAssistantService(GeminiIngredientClient geminiIngredientClient, ProductCatalogClient productCatalogClient) {
        this.geminiIngredientClient = geminiIngredientClient;
        this.productCatalogClient = productCatalogClient;
    }

    public RecommendationResponse recommend(AssistantRequest request) {
        List<ProductCatalogItem> catalogue = productCatalogClient.getProducts();
        IngredientPlan plan = geminiIngredientClient.identifyIngredients(request, catalogue);
        List<RecommendedProduct> recommendations = new ArrayList<>();

        for (IngredientSuggestion ingredient : plan.ingredients()) {
            if (!isValidIngredient(ingredient)) {
                continue;
            }

            ProductCatalogItem product = findProduct(ingredient, catalogue);
            Integer packagesRequired = calculatePackagesRequired(ingredient, product);
            if (product == null || packagesRequired == null || !Boolean.TRUE.equals(product.active())
                    || product.quantity() == null || product.quantity() < packagesRequired) {
                if (Boolean.FALSE.equals(ingredient.mandatory()) && product == null) {
                    continue;
                }
                recommendations.add(new RecommendedProduct(product == null ? null : product.id(),
                        product == null ? null : product.name(), ingredient.name(), ingredient.requiredAmount(),
                        ingredient.unit(), packagesRequired, null, null, "OUT_OF_STOCK"));
                continue;
            }

            Double unitPrice = discountedPrice(product);
            recommendations.add(new RecommendedProduct(product.id(), product.name(), ingredient.name(),
                    ingredient.requiredAmount(), ingredient.unit(), packagesRequired, unitPrice,
                    roundMoney(unitPrice * packagesRequired), "IN_STOCK"));
        }

        Double total = roundMoney(recommendations.stream().filter(product -> "IN_STOCK".equals(product.status()))
                .map(RecommendedProduct::lineTotal).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum());
        Boolean withinBudget = request.budget() == null ? null : total <= request.budget();
        return new RecommendationResponse(plan.dish(), plan.summary(), recommendations, total, request.budget(), withinBudget);
    }

    private boolean isValidIngredient(IngredientSuggestion ingredient) {
        return ingredient != null && ingredient.name() != null && !ingredient.name().isBlank()
                && ingredient.requiredAmount() != null && ingredient.requiredAmount() > 0
                && ingredient.unit() != null && !ingredient.unit().isBlank();
    }

    private ProductCatalogItem findProduct(IngredientSuggestion ingredient, List<ProductCatalogItem> catalogue) {
        if (ingredient.productId() != null) {
            return catalogue.stream().filter(product -> ingredient.productId().equals(product.id())).findFirst().orElse(null);
        }
        String normalizedIngredient = normalize(ingredient.name());
        return catalogue.stream()
                .filter(product -> product.id() != null && product.name() != null && product.price() != null)
                .filter(product -> matches(normalizedIngredient, product))
                .min(Comparator.comparing(this::discountedPrice, Comparator.nullsLast(Double::compareTo)))
                .orElse(null);
    }

    private boolean matches(String ingredient, ProductCatalogItem product) {
        String searchable = String.join(" ", nullToEmpty(product.name()), nullToEmpty(product.brand()),
                nullToEmpty(product.category()), nullToEmpty(product.subCategory()), nullToEmpty(product.description()),
                nullToEmpty(product.tags()), nullToEmpty(product.searchAliases()));
        return normalize(searchable).contains(ingredient);
    }

    private Integer calculatePackagesRequired(IngredientSuggestion ingredient, ProductCatalogItem product) {
        if (product == null || product.unitValue() == null || product.unitValue() <= 0) return null;
        String recipeUnit = normalizeUnit(ingredient.unit());
        String packageUnit = normalizeUnit(product.unitType());
        if (!sameUnitFamily(recipeUnit, packageUnit)) return null;
        Double requiredBase = toBaseUnit(ingredient.requiredAmount(), recipeUnit);
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

    private boolean isWeight(String unit) {
        return "g".equals(unit) || "kg".equals(unit);
    }

    private boolean isVolume(String unit) {
        return "ml".equals(unit) || "l".equals(unit);
    }

    private Double discountedPrice(ProductCatalogItem product) {
        if (product == null || product.price() == null) return null;
        int discount = product.discount() == null ? 0 : Math.max(0, Math.min(100, product.discount()));
        return roundMoney(product.price() * (100 - discount) / 100);
    }

    private Double roundMoney(Double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    private String normalize(String value) {
        return nullToEmpty(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
