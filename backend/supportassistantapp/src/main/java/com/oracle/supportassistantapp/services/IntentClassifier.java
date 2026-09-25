package com.oracle.supportassistantapp.services;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class IntentClassifier {
    public AssistantIntent classify(String message) {
        String value = message.toLowerCase(Locale.ROOT);
        if (contains(value, "recipe", "cook", "meal plan", "ingredients for", "servings")
                || value.matches(".*\\b(plan|prepare|make)\\b.*\\b(person|people|serving|servings)\\b.*")) {
            return AssistantIntent.RECIPE;
        }
        if (contains(value, "cart", "basket", "add item", "remove item", "checkout")) return AssistantIntent.CART;
        if (contains(value, "order", "delivery", "track", "shipped", "delivered", "cancel my")) return AssistantIntent.ORDER;
        if (contains(value, "product request", "pending request", "approved request", "rejected request", "inventory request")) return AssistantIntent.REQUEST;
        if (contains(value, "report", "revenue", "sales", "dashboard", "low stock", "out of stock", "performance")) return AssistantIntent.REPORT;
        if (contains(value, "profile", "password", "fund", "balance", "wishlist", "account", "locked", "sign in", "login", "address")) return AssistantIntent.ACCOUNT;
        if (contains(value, "product", "price", "discount", "deal", "stock", "available", "search", "find", "buy", "do you have")) return AssistantIntent.PRODUCT;
        return AssistantIntent.PLATFORM_HELP;
    }

    public Set<String> meaningfulTokens(String message) {
        Set<String> stopWords = Set.of("show", "find", "search", "please", "product", "products", "item", "items",
                "have", "with", "under", "below", "above", "available", "stock", "price", "want", "need", "give",
                "some", "what", "which", "there", "your", "grocers", "could", "would", "about", "from", "that");
        java.util.LinkedHashSet<String> tokens = new java.util.LinkedHashSet<>();
        for (String token : message.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() >= 2 && !stopWords.contains(token) && !token.matches("\\d+")) tokens.add(token);
        }
        return tokens;
    }

    private boolean contains(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }
}
