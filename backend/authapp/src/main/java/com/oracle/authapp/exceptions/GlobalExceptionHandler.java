package com.oracle.authapp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<Map<String, String>> invalidCredentials(InvalidCredentialsException ignored) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password"));
    }

    @ExceptionHandler(AccountLockedException.class)
    ResponseEntity<Map<String, String>> accountLocked(AccountLockedException ignored) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "This user account is locked"));
    }

    @ExceptionHandler(EmployeeInactiveException.class)
    ResponseEntity<Map<String, String>> employeeInactive(EmployeeInactiveException ignored) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "This employee account is inactive"));
    }

    @ExceptionHandler(SecurityRecoveryEscalatedException.class)
    ResponseEntity<Map<String, String>> securityRecoveryEscalated(SecurityRecoveryEscalatedException ignored) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Security recovery needs employee review"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
