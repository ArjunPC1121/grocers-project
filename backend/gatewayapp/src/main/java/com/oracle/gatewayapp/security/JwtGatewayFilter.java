package com.oracle.gatewayapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Stateless gateway authentication: it verifies the JWT locally and never calls
 * AuthApp or Oracle. Downstream identity headers are removed first so callers
 * cannot forge them when requests pass through this gateway.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtGatewayFilter implements GlobalFilter {
    private static final String USER_ID_HEADER = "X-Authenticated-User-Id";
    private static final String EMAIL_HEADER = "X-Authenticated-User-Email";
    private static final String ROLE_HEADER = "X-Authenticated-Role";
    private static final String MUST_CHANGE_PASSWORD_HEADER = "X-Must-Change-Password";
    private static final String GATEWAY_REQUEST_HEADER = "X-Gateway-Request";
    private final SecretKey signingKey;
    private final String internalRequestSecret;

    private static final String INTERNAL_SERVICE_HEADER = "X-Internal-Service";
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    public JwtGatewayFilter(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.gateway.internal-secret}") String internalRequestSecret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.internalRequestSecret = internalRequestSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isAuthAppFailedAttemptRequest(exchange)) {
            return chain.filter(exchange);
        }
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS || isPublic(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(authorization.substring(7))
                    .getPayload();
            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);
            Boolean mustChangePassword = claims.get("mustChangePassword", Boolean.class);

            if (userId == null || email == null || role == null || !isAuthorized(path, role)
                    || (Boolean.TRUE.equals(mustChangePassword) && !isAllowedWhilePasswordChangeIsRequired(path, exchange.getRequest().getMethod()))) {
                return reject(exchange, HttpStatus.FORBIDDEN);
            }

            ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
                headers.remove(USER_ID_HEADER);
                headers.remove(EMAIL_HEADER);
                headers.remove(ROLE_HEADER);
                headers.remove(MUST_CHANGE_PASSWORD_HEADER);
                headers.remove(GATEWAY_REQUEST_HEADER);
                headers.set(USER_ID_HEADER, userId);
                headers.set(EMAIL_HEADER, email);
                headers.set(ROLE_HEADER, role);
                headers.set(MUST_CHANGE_PASSWORD_HEADER, Boolean.toString(Boolean.TRUE.equals(mustChangePassword)));
                headers.set(GATEWAY_REQUEST_HEADER, internalRequestSecret);
            }).build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (JwtException | IllegalArgumentException exception) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublic(String path) {

        return path.startsWith("/grocers/api/auth/login/")
                || path.equals("/grocers/api/users");
    }

    private boolean isAuthorized(String path, String role) {
        if (path.equals("/grocers/api/users/admin")) {
            return "ADMIN".equals(role);
        }
        if (path.matches("/grocers/api/users/\\d+/(failed-attempts|unlock)")) {
            return Set.of("EMPLOYEE", "ADMIN").contains(role);
        }

        if (path.startsWith("/grocers/api/admin") || path.startsWith("/grocers/api/admins")) {
            return "ADMIN".equals(role);
        }
        if (path.startsWith("/grocers/api/employee") || path.startsWith("/grocers/api/employees")) {
            return Set.of("EMPLOYEE", "ADMIN").contains(role);
        }
        if (path.startsWith("/grocers/api/requests")) {
            return Set.of("EMPLOYEE", "ADMIN").contains(role);
        }
        return Set.of("USER", "EMPLOYEE", "ADMIN").contains(role);
    }

    private boolean isAllowedWhilePasswordChangeIsRequired(String path, HttpMethod method) {
        return (method == HttpMethod.PUT || method == HttpMethod.PATCH)
                && path.matches(".*/employees/\\d+/password$");
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private boolean isAuthAppFailedAttemptRequest(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();

        return exchange.getRequest().getMethod() == HttpMethod.POST
                && path.matches("/grocers/api/users/\\d+/failed-attempts")
                && "authapp".equals(
                exchange.getRequest().getHeaders()
                        .getFirst(INTERNAL_SERVICE_HEADER))
                && internalRequestSecret.equals(
                exchange.getRequest().getHeaders()
                        .getFirst(INTERNAL_SECRET_HEADER));
    }
}
