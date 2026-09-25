package com.oracle.assistantapp.services;

import com.oracle.assistantapp.dto.IngredientSuggestion;
import com.oracle.assistantapp.dto.ProductCatalogItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Performs conservative, deterministic matching from a recipe requirement to a catalogue item. */
@Component
public class CatalogueIngredientMatcher {
    private static final Set<String> STOP_WORDS = Set.of("fresh", "premium", "organic", "pack", "the", "and", "for", "with");

    public MatchResult findBestMatch(IngredientSuggestion ingredient, List<ProductCatalogItem> catalogue) {
        List<ScoredProduct> matches = new ArrayList<>();
        for (ProductCatalogItem product : catalogue) {
            if (product == null || product.id() == null || product.name() == null || product.price() == null) continue;
            int score = relevanceScore(ingredient, product);
            if (score > 0) matches.add(new ScoredProduct(product, score));
        }
        if (matches.isEmpty()) return new MatchResult(null, "NO_CATALOG_MATCH");

        matches.sort(Comparator.comparingInt(ScoredProduct::score).reversed()
                .thenComparing((ScoredProduct candidate) -> hasCompatibleUnit(ingredient, candidate.product()) ? 0 : 1)
                .thenComparing((ScoredProduct candidate) -> Boolean.TRUE.equals(candidate.product().active()) ? 0 : 1)
                .thenComparing((ScoredProduct candidate) -> isInStock(candidate.product()) ? 0 : 1)
                .thenComparing(candidate -> discountedPrice(candidate.product()), Comparator.nullsLast(Double::compareTo))
                .thenComparing(candidate -> candidate.product().id()));
        return new MatchResult(matches.get(0).product(), "MATCHED");
    }

    private int relevanceScore(IngredientSuggestion ingredient, ProductCatalogItem product) {
        String canonical = normalize(ingredient.canonicalName());
        String productName = normalize(product.name());
        String searchable = normalize(String.join(" ", nullToEmpty(product.name()), nullToEmpty(product.brand()),
                nullToEmpty(product.category()), nullToEmpty(product.subCategory()), nullToEmpty(product.description()),
                nullToEmpty(product.tags()), nullToEmpty(product.searchAliases())));
        if (canonical.isBlank() || searchable.isBlank()) return 0;
        if (productName.equals(canonical)) return 1000;

        int best = phraseScore(canonical, searchable, productName);
        if (ingredient.matchTerms() != null) {
            for (String matchTerm : ingredient.matchTerms()) {
                best = Math.max(best, phraseScore(normalize(matchTerm), searchable, productName));
            }
        }
        return best;
    }

    private int phraseScore(String term, String searchable, String productName) {
        if (term.isBlank()) return 0;
        if (productName.contains(term)) return 900;
        if (searchable.contains(term)) return 750;
        Set<String> termTokens = tokens(term);
        Set<String> productTokens = tokens(productName);
        return termTokens.size() >= 2 && productTokens.containsAll(termTokens) ? 700 : 0;
    }

    private Set<String> tokens(String value) {
        return List.of(value.split("\\s+")).stream()
                .map(this::stem)
                .filter(token -> token.length() > 1 && !STOP_WORDS.contains(token))
                .collect(Collectors.toSet());
    }

    private String stem(String token) {
        if (token.endsWith("ies") && token.length() > 4) return token.substring(0, token.length() - 3) + "y";
        if (token.endsWith("ed") && token.length() > 4) return token.substring(0, token.length() - 2);
        if (token.endsWith("s") && token.length() > 3) return token.substring(0, token.length() - 1);
        return token;
    }

    private boolean isInStock(ProductCatalogItem product) {
        return Boolean.TRUE.equals(product.active()) && product.quantity() != null && product.quantity() > 0;
    }

    private boolean hasCompatibleUnit(IngredientSuggestion ingredient, ProductCatalogItem product) {
        if (product.unitValue() == null || product.unitValue() <= 0 || product.unitType() == null) return false;
        String packageUnit = normalize(product.unitType());
        return switch (ingredient.unit()) {
            case G, KG -> packageUnit.equals("g") || packageUnit.equals("gm") || packageUnit.equals("gram")
                    || packageUnit.equals("grams") || packageUnit.equals("kg") || packageUnit.equals("kgs")
                    || packageUnit.equals("kilogram") || packageUnit.equals("kilograms");
            case ML, L -> packageUnit.equals("ml") || packageUnit.equals("millilitre") || packageUnit.equals("millilitres")
                    || packageUnit.equals("milliliter") || packageUnit.equals("milliliters") || packageUnit.equals("l")
                    || packageUnit.equals("lt") || packageUnit.equals("litre") || packageUnit.equals("litres")
                    || packageUnit.equals("liter") || packageUnit.equals("liters");
            case UNIT -> packageUnit.equals("unit") || packageUnit.equals("units") || packageUnit.equals("piece")
                    || packageUnit.equals("pieces") || packageUnit.equals("pc") || packageUnit.equals("pcs") || packageUnit.equals("count");
        };
    }

    private Double discountedPrice(ProductCatalogItem product) {
        int discount = product.discount() == null ? 0 : Math.max(0, Math.min(100, product.discount()));
        return product.price() * (100 - discount) / 100;
    }

    private String normalize(String value) {
        return nullToEmpty(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record ScoredProduct(ProductCatalogItem product, int score) { }
    public record MatchResult(ProductCatalogItem product, String reason) { }
}
