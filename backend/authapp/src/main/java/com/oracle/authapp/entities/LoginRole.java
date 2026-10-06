/**
 * Component role: Maps a domain concept to persistent storage and contains the state that the service owns.
 *
 * Maintainer note: this file belongs to authapp. See backend/authapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.authapp.entities;

public enum LoginRole {
    USER,
    EMPLOYEE,
    ADMIN
}
