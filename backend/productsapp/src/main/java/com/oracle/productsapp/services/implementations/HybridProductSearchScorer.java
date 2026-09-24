package com.oracle.productsapp.services.implementations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.oracle.productsapp.dtos.ProductSearchCandidate;
import com.oracle.productsapp.dtos.ProductSearchResult;

@Component
public class HybridProductSearchScorer {

    public ProductSearchResult score(
            ProductSearchCandidate candidate,
            String normalizedQuery,
            List<String> queryTokens,
            boolean fuzzySearchEnabled
    ) {
        float semanticScore = candidate.oracleSemanticScore() == null
                ? 0
                : clamp(candidate.oracleSemanticScore().floatValue());
        float structuredScore = structuredLexicalScore(candidate, normalizedQuery);
        structuredScore = Math.max(
                structuredScore,
                tokenCoverageScore(candidate, queryTokens)
        );
        float lexicalScore = Math.max(normalizedOracleTextScore(candidate.oracleTextScore()), structuredScore);
        if (fuzzySearchEnabled && structuredScore == 0
                && !hasExactTokenMatch(candidate, normalizedQuery)) {
            lexicalScore = fuzzyStructuredLexicalScore(candidate, normalizedQuery);
        }
        float finalScore = finalScore(lexicalScore, semanticScore);

        return new ProductSearchResult(
                candidate.id(), candidate.name(), candidate.brand(),
                candidate.category(), candidate.subCategory(),
                candidate.description(), candidate.imageUrl(), candidate.price(),
                candidate.discount(), candidate.quantity(),
                BigDecimal.valueOf(lexicalScore),
                BigDecimal.valueOf(semanticScore),
                BigDecimal.valueOf(finalScore)
        );
    }

    private float finalScore(float lexicalScore, float semanticScore) {
        if (lexicalScore >= 0.88F) {
            return lexicalScore;
        }
        if (lexicalScore > 0 && semanticScore > 0) {
            return (lexicalScore * 0.60F) + (semanticScore * 0.40F);
        }
        return lexicalScore > 0 ? lexicalScore : semanticScore * 0.75F;
    }

    private float normalizedOracleTextScore(BigDecimal score) {
        return score == null ? 0 : clamp(score.floatValue() / 100F);
    }

    private float structuredLexicalScore(ProductSearchCandidate candidate, String query) {
        String name = lower(candidate.name());
        String brand = lower(candidate.brand());
        String category = lower(candidate.category());
        String subCategory = lower(candidate.subCategory());

        if (name.equals(query)) return 1.00F;
        if ((brand + " " + name).trim().equals(query)) return 0.99F;
        if (brand.equals(query)) return 0.95F;
        if (containsCommaSeparated(candidate.searchAliases(), query)) return 0.93F;
        if (subCategory.equals(query)) return 0.91F;
        if (containsCommaSeparated(candidate.tags(), query)) return 0.90F;
        if (category.equals(query)) return 0.88F;
        if (name.startsWith(query)) return 0.86F;
        if (brand.startsWith(query)) return 0.83F;
        if (subCategory.startsWith(query)) return 0.81F;
        if (category.startsWith(query)) return 0.79F;
        return 0;
    }

    /**
     * Oracle Text's FUZZY operator is a candidate generator, not sufficient
     * relevance proof.  Accept a fuzzy-only hit only when the query is within
     * a conservative edit distance of a product-facing field.
     */
    private float fuzzyStructuredLexicalScore(ProductSearchCandidate candidate, String query) {
        int maximumDistance = query.length() >= 7 ? 2 : 1;
        for (String value : new String[] {
                candidate.name(), candidate.brand(), candidate.category(),
                candidate.subCategory(), candidate.searchAliases(), candidate.tags()
        }) {
            if (value == null) continue;
            for (String token : value.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
                if (!token.isBlank() && damerauLevenshteinDistance(query, token) <= maximumDistance) {
                    return 0.78F;
                }
            }
        }
        return 0;
    }

    private boolean hasExactTokenMatch(ProductSearchCandidate candidate, String query) {
        for (String value : new String[] {
                candidate.name(), candidate.brand(), candidate.category(),
                candidate.subCategory(), candidate.description(), candidate.searchAliases(), candidate.tags()
        }) {
            if (value == null) continue;
            for (String token : value.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
                if (token.equals(query)) return true;
            }
        }
        return false;
    }

    /** Scores a partial lexical hit above a semantic-only result. */
    private float tokenCoverageScore(ProductSearchCandidate candidate, List<String> queryTokens) {
        if (queryTokens.isEmpty()) return 0;
        String searchable = String.join(" ", new String[] {
                lower(candidate.name()), lower(candidate.brand()), lower(candidate.category()),
                lower(candidate.subCategory()), lower(candidate.description()),
                lower(candidate.searchAliases()), lower(candidate.tags())
        });
        long matched = queryTokens.stream()
                .filter(token -> searchable.matches(".*(?s)(^|[^\\p{L}\\p{N}])"
                        + java.util.regex.Pattern.quote(token)
                        + "([^\\p{L}\\p{N}]|$).*"))
                .count();
        if (matched == 0) return 0;
        return 0.45F + (0.40F * ((float) matched / queryTokens.size()));
    }

    private int damerauLevenshteinDistance(String left, String right) {
        if (Math.abs(left.length() - right.length()) > 2) return 3;
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        int[] previousPrevious = new int[right.length() + 1];
        for (int index = 0; index <= right.length(); index++) previous[index] = index;
        for (int leftIndex = 1; leftIndex <= left.length(); leftIndex++) {
            current[0] = leftIndex;
            for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++) {
                int replacementCost = left.charAt(leftIndex - 1) == right.charAt(rightIndex - 1) ? 0 : 1;
                current[rightIndex] = Math.min(
                        Math.min(current[rightIndex - 1] + 1, previous[rightIndex] + 1),
                        previous[rightIndex - 1] + replacementCost
                );
                if (leftIndex > 1 && rightIndex > 1
                        && left.charAt(leftIndex - 1) == right.charAt(rightIndex - 2)
                        && left.charAt(leftIndex - 2) == right.charAt(rightIndex - 1)) {
                    current[rightIndex] = Math.min(current[rightIndex], previousPrevious[rightIndex - 2] + 1);
                }
            }
            previousPrevious = previous;
            previous = current;
            current = new int[right.length() + 1];
        }
        return previous[right.length()];
    }

    private boolean containsCommaSeparated(String value, String query) {
        if (value == null || value.isBlank()) return false;
        for (String item : value.split(",")) {
            if (item.trim().equalsIgnoreCase(query)) return true;
        }
        return false;
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private float clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }
}
