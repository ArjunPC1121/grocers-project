/**
 * Component role: Maps a domain concept to persistent storage and contains the state that the service owns.
 *
 * Maintainer note: this file belongs to requestapp. See backend/requestapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.requestapp.entities;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Mirrors the fixed catalogue categories accepted by productsapp. */
public enum ProductCategory {
    FRUITS_AND_VEGETABLES("Fruits & Vegetables"), PERSONAL_CARE("Personal Care"),
    PANTRY_STAPLES("Pantry Staples"), BAKERY("Bakery"), BEVERAGES("Beverages"),
    MEAT_AND_SEAFOOD("Meat & Seafood"), SNACKS("Snacks"), FROZEN_FOODS("Frozen Foods"),
    BABY_CARE("Baby Care"), DAIRY_AND_EGGS("Dairy & Eggs");

    private final String displayName;

    private static final Map<String, ProductCategory> LEGACY_ALIASES = Map.ofEntries(
            Map.entry("fruit", FRUITS_AND_VEGETABLES),
            Map.entry("fruits", FRUITS_AND_VEGETABLES),
            Map.entry("vegetable", FRUITS_AND_VEGETABLES),
            Map.entry("vegetables", FRUITS_AND_VEGETABLES),
            Map.entry("produce", FRUITS_AND_VEGETABLES),
            Map.entry("dairy", DAIRY_AND_EGGS),
            Map.entry("eggs", DAIRY_AND_EGGS),
            Map.entry("meat", MEAT_AND_SEAFOOD),
            Map.entry("seafood", MEAT_AND_SEAFOOD),
            Map.entry("drinks", BEVERAGES),
            Map.entry("beverage", BEVERAGES),
            Map.entry("frozen", FROZEN_FOODS),
            Map.entry("pantry", PANTRY_STAPLES),
            Map.entry("oil", PANTRY_STAPLES),
            Map.entry("oils", PANTRY_STAPLES),
            Map.entry("cooking oil", PANTRY_STAPLES),
            Map.entry("cooking oils", PANTRY_STAPLES),
            Map.entry("personal", PERSONAL_CARE),
            Map.entry("baby", BABY_CARE)
    );

    ProductCategory(String displayName) { this.displayName = displayName; }

    @JsonValue
    public String getDisplayName() { return displayName; }

    @JsonCreator
    public static ProductCategory from(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = normalize(value);
        ProductCategory legacyCategory = LEGACY_ALIASES.get(normalized);
        if (legacyCategory != null) return legacyCategory;

        return Arrays.stream(values())
                .filter(category -> normalize(category.name()).equals(normalized)
                        || normalize(category.displayName).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported category: " + value));
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("&", "and")
                .replaceAll("[^a-z0-9]+", "").trim().replace(" ", "");
    }
}
