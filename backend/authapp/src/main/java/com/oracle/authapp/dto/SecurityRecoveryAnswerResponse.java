/**
 * Component role: Defines a transport contract used at an API or service boundary. Keep it free of persistence and business side effects.
 *
 * Maintainer note: this file belongs to authapp. See backend/authapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.authapp.dto;

/** Short-lived proof returned only after the correct security answer. */
public record SecurityRecoveryAnswerResponse(String resetToken) { }
