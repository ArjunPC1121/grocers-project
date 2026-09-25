package com.oracle.productsapp.services.implementations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.oracle.productsapp.dtos.ProductSearchCandidate;

class ProductSearchQueryAnalyzerTest {

    private final ProductSearchQueryAnalyzer analyzer = new ProductSearchQueryAnalyzer();

    @Test
    void doesNotRouteReportedNoiseToFuzzyOrSemanticSearch() {
        for (String query : new String[] { "dd", "12", "emms", "kij", "aaa" }) {
            ProductSearchQuery plan = analyzer.analyze(query);
            assertFalse(plan.allowSemanticSearch(), query);
            assertFalse(plan.allowFuzzySearch(), query);
        }
    }

    @Test
    void doesNotTreatAnUnrelatedFuzzyCandidateAsALexicalMatch() {
        ProductSearchCandidate milk = new ProductSearchCandidate(
                1, "Milk", null, "Dairy", null, null, null, 1D, 0, 1,
                null, null, new float[] { 1F }, BigDecimal.valueOf(90)
        );

        assertTrue(analyzer.analyze("jislwl").allowFuzzySearch());
        assertTrue(new HybridProductSearchScorer()
                .score(milk, "jislwl", java.util.List.of("jislwl"), null, true)
                .lexicalScore().signum() == 0);
    }

    @Test
    void keepsNaturalLanguageQueriesEligibleForSemanticSearch() {
        ProductSearchQuery plan = analyzer.analyze("gluten free snacks");
        assertTrue(plan.allowSemanticSearch());
        assertFalse(plan.allowFuzzySearch());
    }

    @Test
    void ignoresConnectorWordsInNaturalLanguageProductIntent() {
        ProductSearchQuery plan = analyzer.analyze("eggs to make omelette");
        assertTrue(plan.allowSemanticSearch());
        assertTrue(plan.tokens().contains("eggs"));
        assertTrue(plan.tokens().contains("omelette"));
        assertFalse(plan.tokens().contains("to"));
        assertFalse(plan.tokens().contains("make"));
    }

    @Test
    void keepsPlausibleSingleWordTyposEligibleForConservativeFuzzyMatching() {
        ProductSearchQuery plan = analyzer.analyze("milkk");
        assertTrue(plan.allowFuzzySearch());
        assertFalse(plan.allowSemanticSearch());
    }
}
