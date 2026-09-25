/**
 * Component role: Defines the HTTP boundary for this service. It accepts transport input, reads trusted gateway identity headers where required, and delegates business work to the service layer.
 *
 * Maintainer note: this file belongs to authapp. See backend/authapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.authapp.controllers;

import com.oracle.authapp.dto.AuthResponse;
import com.oracle.authapp.dto.LoginRequest;
import com.oracle.authapp.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/grocers/api/auth/login")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/user")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        // Customer login can update UserApp's failed-attempt counter on a bad password.
        return ResponseEntity.ok(authService.loginUser(request));
    }

    @PostMapping("/employee")
    public ResponseEntity<AuthResponse> loginEmployee(@Valid @RequestBody LoginRequest request) {
        // Employee sign-in additionally checks the active/inactive employment state.
        return ResponseEntity.ok(authService.loginEmployee(request));
    }

    @PostMapping("/admin")
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody LoginRequest request) {
        // Admin permissions are later enforced by Gateway and AdminApp from this JWT role.
        return ResponseEntity.ok(authService.loginAdmin(request));
    }
}
