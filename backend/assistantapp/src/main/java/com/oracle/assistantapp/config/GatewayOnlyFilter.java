package com.oracle.assistantapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Keeps identity headers trustworthy by accepting requests only from Gateway. */
@Component
public class GatewayOnlyFilter extends OncePerRequestFilter {
    private final String internalRequestSecret;

    public GatewayOnlyFilter(@Value("${app.gateway.internal-secret}") String internalRequestSecret) {
        this.internalRequestSecret = internalRequestSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!internalRequestSecret.equals(request.getHeader("X-Gateway-Request"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Assistant APIs must be called through the gateway");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
