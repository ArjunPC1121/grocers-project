package com.oracle.chatapp.config;

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

@Component @Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayOnlyFilter extends OncePerRequestFilter {
  private final String secret;
  public GatewayOnlyFilter(@Value("${app.gateway.internal-secret}") String secret) { this.secret = secret; }
  @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    if (!secret.equals(request.getHeader("X-Gateway-Request"))) { response.sendError(403, "Chat APIs must be called through the gateway"); return; }
    chain.doFilter(request, response);
  }
}
