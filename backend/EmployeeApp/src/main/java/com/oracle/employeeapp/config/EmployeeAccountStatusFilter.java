package com.oracle.employeeapp.config;

import com.oracle.employeeapp.entities.EmployeeStatus;
import com.oracle.employeeapp.repositories.EmployeeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Revokes EmployeeApp access immediately when an employee account is deactivated. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class EmployeeAccountStatusFilter extends OncePerRequestFilter {
    private final EmployeeRepository employees;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!"EMPLOYEE".equals(request.getHeader("X-Authenticated-Role"))) {
            filterChain.doFilter(request, response);
            return;
        }

        String employeeId = request.getHeader("X-Authenticated-User-Id");
        if (employeeId == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Employee identity is required");
            return;
        }

        if (isOwnProfileRequest(request, employeeId)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            boolean active = employees.findById(Integer.valueOf(employeeId))
                    .map(employee -> employee.getStatus() == EmployeeStatus.ACTIVE)
                    .orElse(false);
            if (!active) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "This employee account is deactivated");
                return;
            }
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid employee identity");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isOwnProfileRequest(HttpServletRequest request, String employeeId) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().matches(".*/employees/" + employeeId + "$");
    }
}
