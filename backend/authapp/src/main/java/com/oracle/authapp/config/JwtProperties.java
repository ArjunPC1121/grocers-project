/**
 * Component role: Configures a cross-cutting concern such as security, HTTP clients, serialization, or application startup behaviour.
 *
 * Maintainer note: this file belongs to authapp. See backend/authapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.authapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long userExpirationMs,
        long employeeExpirationMs,
        long adminExpirationMs
) { }
