package com.oracle.productsapp.services.implementations;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.oracle.productsapp.dtos.ProductSearchCandidate;
import com.oracle.productsapp.dtos.ProductSearchResult;

@Component
public class HybridProductSearchScorer {

    public ProductSearchResult score(
            ProductSearchCandidate candidate,
            String normalizedQuery,
            float[] queryEmbedding
    ) {
        float semanticScore = cosineSimilarity(queryEmbedding, candidate.embedding());
        float lexicalScore = Math.max(
                normalizedOracleTextScore(candidate.oracleTextScore()),
                structuredLexicalScore(candidate, normalizedQuery)
        );
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

    private float cosineSimilarity(float[] query, float[] candidate) {
        if (candidate == null || query.length != candidate.length) {
            return 0;
        }

        double dotProduct = 0;
        double queryMagnitude = 0;
        double candidateMagnitude = 0;
        for (int index = 0; index < query.length; index++) {
            dotProduct += query[index] * candidate[index];
            queryMagnitude += query[index] * query[index];
            candidateMagnitude += candidate[index] * candidate[index];
        }

        if (queryMagnitude == 0 || candidateMagnitude == 0) {
            return 0;
        }

        return clamp((float) (dotProduct / Math.sqrt(queryMagnitude * candidateMagnitude)));
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
