package com.oracle.employeeapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Rejects requests that did not originate at the API gateway. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayOnlyFilter extends OncePerRequestFilter {
    private static final String GATEWAY_HEADER = "X-Gateway-Request";
    private final String internalRequestSecret;

    public GatewayOnlyFilter(@Value("${app.gateway.internal-secret}") String internalRequestSecret) {
        this.internalRequestSecret = internalRequestSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!internalRequestSecret.equals(request.getHeader(GATEWAY_HEADER))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Employee APIs must be called through the gateway");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
