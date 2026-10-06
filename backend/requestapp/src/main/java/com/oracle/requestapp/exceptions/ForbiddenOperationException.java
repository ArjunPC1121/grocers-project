/**
 * Component role: Represents or maps an expected failure into a clear API response. Controllers should not need to duplicate this error-handling policy.
 *
 * Maintainer note: this file belongs to requestapp. See backend/requestapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.requestapp.exceptions;

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) { super(message); }
}
