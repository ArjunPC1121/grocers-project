package com.oracle.productsapp.services.implementations;

import java.util.List;

/**
 * A normalized query and the retrieval paths that are safe to use for it.
 *
 * <p>Dense embeddings always return a nearest neighbour, including for random
 * characters.  Keeping this decision separate from scoring prevents a noisy
 * query from turning the whole catalogue into semantic candidates.</p>
 */
public record ProductSearchQuery(
        String normalizedQuery,
        List<String> tokens,
        boolean allowFuzzySearch,
        boolean allowSemanticSearch
) {}
