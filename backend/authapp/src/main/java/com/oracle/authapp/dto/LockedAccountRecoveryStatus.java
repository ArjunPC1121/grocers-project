/**
 * Component role: Defines a transport contract used at an API or service boundary. Keep it free of persistence and business side effects.
 *
 * Maintainer note: this file belongs to authapp. See backend/authapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.authapp.dto;

/** Indicates whether employee-assisted recovery has restored access to a locked account. */
public record LockedAccountRecoveryStatus(boolean unlocked, boolean ticketOpen) { }
