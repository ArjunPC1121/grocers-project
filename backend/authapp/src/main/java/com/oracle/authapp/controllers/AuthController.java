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
        return ResponseEntity.ok(authService.loginUser(request));
    }

    @PostMapping("/employee")
    public ResponseEntity<AuthResponse> loginEmployee(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginEmployee(request));
    }

    @PostMapping("/admin")
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginAdmin(request));
    }
}
