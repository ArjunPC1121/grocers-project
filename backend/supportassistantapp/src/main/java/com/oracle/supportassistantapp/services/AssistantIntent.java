/**
 * Component role: Coordinates this service's business workflow, including validation, authorization decisions, persistence, and downstream integration where applicable.
 *
 * Maintainer note: this file belongs to supportassistantapp. See backend/supportassistantapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.supportassistantapp.services;

public enum AssistantIntent {
    PRODUCT,
    ORDER,
    CART,
    REQUEST,
    REPORT,
    ACCOUNT,
    RECIPE,
    PLATFORM_HELP
}
