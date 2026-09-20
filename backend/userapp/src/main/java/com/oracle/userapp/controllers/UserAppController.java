package com.oracle.userapp.controllers;

import com.oracle.userapp.dto.*;
import com.oracle.userapp.services.implementations.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/grocers/api/users")
public class UserAppController {

    private final UserService userService;

    public UserAppController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> add(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.add(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Collection<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.get(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UserResponse> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.delete(id));
    }

    @PostMapping("/{id}/failed-attempts")
    public ResponseEntity<Integer> incrementFailedAttempts(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.incFailedAttempts(id));
    }

    @PostMapping("/{id}/funds")
    public ResponseEntity<Double> addFunds(
            @PathVariable Integer id,
            @RequestBody AddFundsRequest request) {
        return ResponseEntity.ok(userService.addFunds(id, request.amount()));
    }

    @PostMapping("/{id}/deductFunds")
    public ResponseEntity<Double> deductFunds(
            @PathVariable Integer id,
            @RequestBody AddFundsRequest request) {
        return ResponseEntity.ok(userService.deductFunds(id, request.amount()));
    }

    @PostMapping("/{id}/tickets")
    public ResponseEntity<TicketResponse> raiseTicket(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.raiseTicket(id));
    }

    /** Called by ticketapp after an employee resolves a lockout ticket. */
    @PostMapping("/{id}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable Integer id) {
        userService.unlockAccount(id);
        return ResponseEntity.noContent().build();
    }


}
