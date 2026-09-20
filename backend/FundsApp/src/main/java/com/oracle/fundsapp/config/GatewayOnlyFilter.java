package com.oracle.fundsapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Funds mutations may originate only from the gateway or a trusted backend service. */
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
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Funds APIs must be called through a trusted service");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
