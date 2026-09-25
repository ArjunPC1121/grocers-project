/**
 * Component role: Configures a cross-cutting concern such as security, HTTP clients, serialization, or application startup behaviour.
 *
 * Maintainer note: this file belongs to authapp. See backend/authapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.authapp.security;

import com.oracle.authapp.config.JwtProperties;
import com.oracle.authapp.entities.LoginRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Integer accountId, String email, LoginRole role, boolean mustChangePassword) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(expirationFor(role));
        return Jwts.builder()
                .subject(accountId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("mustChangePassword", mustChangePassword)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public long expirationFor(LoginRole role) {
        return switch (role) {
            case USER -> properties.userExpirationMs();
            case EMPLOYEE -> properties.employeeExpirationMs();
            case ADMIN -> properties.adminExpirationMs();
        };
    }
}
