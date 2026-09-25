/**
 * Component role: Coordinates this service's business workflow, including validation, authorization decisions, persistence, and downstream integration where applicable.
 *
 * Maintainer note: this file belongs to supportassistantapp. See backend/supportassistantapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.supportassistantapp.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntentClassifierTests {
    @Test void keepsRecipePlannerSeparate() throws Exception {
        assertEquals("RECIPE", classify("Plan paneer butter masala for four people"));
    }

    @Test void recognisesLiveCustomerQuestions() throws Exception {
        assertEquals("ORDER", classify("Where is my latest order?"));
        assertEquals("PRODUCT", classify("Find rice below 800"));
        assertEquals("ACCOUNT", classify("What is my available balance?"));
    }

    private String classify(String message) throws Exception {
        Class<?> type = Class.forName("com.oracle.supportassistantapp.services.IntentClassifier");
        Object classifier = type.getConstructor().newInstance();
        return type.getMethod("classify", String.class).invoke(classifier, message).toString();
    }
}
