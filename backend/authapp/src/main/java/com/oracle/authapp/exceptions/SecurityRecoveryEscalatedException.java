/**
 * Component role: Configures a cross-cutting concern such as security, HTTP clients, serialization, or application startup behaviour.
 *
 * Maintainer note: this file belongs to authapp. See backend/authapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.authapp.exceptions;

/** The customer used or failed self-service recovery and now needs employee review. */
public class SecurityRecoveryEscalatedException extends RuntimeException { }
