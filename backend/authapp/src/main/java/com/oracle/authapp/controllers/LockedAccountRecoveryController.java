package com.oracle.authapp.controllers;

import com.oracle.authapp.dto.LockedAccountTicketRequest;
import com.oracle.authapp.dto.LockedAccountRecoveryStatus;
import com.oracle.authapp.services.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public recovery entry point for customers who cannot authenticate because their account is locked. */
@RestController
@RequestMapping("/grocers/api/auth/locked-account")
public class LockedAccountRecoveryController {
    private final AuthService authService;

    public LockedAccountRecoveryController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/ticket")
    public ResponseEntity<Void> raiseTicket(@Valid @RequestBody LockedAccountTicketRequest request) {
        authService.raiseLockedAccountTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/status")
    public ResponseEntity<LockedAccountRecoveryStatus> status(@RequestParam @NotBlank @Email String email) {
        return ResponseEntity.ok(authService.lockedAccountStatus(email));
    }
}
