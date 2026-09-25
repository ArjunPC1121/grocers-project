/**
 * Component role: Defines the HTTP boundary for this service. It accepts transport input, reads trusted gateway identity headers where required, and delegates business work to the service layer.
 *
 * Maintainer note: this file belongs to authapp. See backend/authapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.authapp.controllers;

import com.oracle.authapp.dto.LockedAccountTicketRequest;
import com.oracle.authapp.dto.LockedAccountRecoveryStatus;
import com.oracle.authapp.dto.SecurityRecoveryAnswerRequest;
import com.oracle.authapp.dto.SecurityRecoveryAnswerResponse;
import com.oracle.authapp.dto.SecurityRecoveryResetPasswordRequest;
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
        // This is the assisted fallback after security-question recovery cannot proceed.
        authService.raiseLockedAccountTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/status")
    public ResponseEntity<LockedAccountRecoveryStatus> status(@RequestParam @NotBlank @Email String email) {
        // The UI uses this to choose between self-service recovery and ticket tracking.
        return ResponseEntity.ok(authService.lockedAccountStatus(email));
    }

    @GetMapping("/security-question")
    public ResponseEntity<String> securityQuestion(@RequestParam @NotBlank @Email String email) { return ResponseEntity.ok(authService.securityQuestion(email)); }

    @PostMapping("/verify-security-answer")
    public ResponseEntity<SecurityRecoveryAnswerResponse> verifySecurityAnswer(@Valid @RequestBody SecurityRecoveryAnswerRequest request) {
        // A successful answer returns a short-lived reset token; it is not a login token.
        return ResponseEntity.ok(authService.verifySecurityAnswer(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody SecurityRecoveryResetPasswordRequest request) {
        authService.resetPasswordFromSecurityQuestion(request);
        return ResponseEntity.noContent().build();
    }
}
