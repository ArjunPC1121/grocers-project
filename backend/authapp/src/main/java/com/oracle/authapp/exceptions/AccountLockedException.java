/**
 * Component role: Represents or maps an expected failure into a clear API response. Controllers should not need to duplicate this error-handling policy.
 *
 * Maintainer note: this file belongs to authapp. See backend/authapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.authapp.exceptions;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException() {
        super("This user account is locked and cannot sign in");
    }
}
