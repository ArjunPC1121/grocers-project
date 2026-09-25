/**
 * Component role: Defines the HTTP boundary for this service. It accepts transport input, reads trusted gateway identity headers where required, and delegates business work to the service layer.
 *
 * Maintainer note: this file belongs to requestapp. See backend/requestapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.requestapp.controllers;

import com.oracle.requestapp.dto.CreateProductRequest;
import com.oracle.requestapp.dto.ProductRequestResponse;
import com.oracle.requestapp.dto.UpdateRequestStatus;
import com.oracle.requestapp.entities.RequestAction;
import com.oracle.requestapp.entities.RequestStatus;
import com.oracle.requestapp.exceptions.ForbiddenOperationException;
import com.oracle.requestapp.services.abstractions.ProductRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/grocers/api/requests")
public class ProductRequestController {
    private final ProductRequestService service;

    public ProductRequestController(ProductRequestService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ProductRequestResponse> create(
            @RequestHeader("X-Authenticated-User-Id") Integer employeeId,
            @RequestHeader("X-Authenticated-Role") String role,
            @Valid @RequestBody CreateProductRequest request) {
        // Gateway supplies these identity headers after JWT validation. Employees can
        // create a request only for themselves; no employee ID comes from the body.
        requireRole(role, "EMPLOYEE");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(employeeId, request));
    }

    @GetMapping("/my")
    public List<ProductRequestResponse> mine(@RequestHeader("X-Authenticated-User-Id") Integer employeeId,
                                              @RequestHeader("X-Authenticated-Role") String role,
                                              @RequestParam(required = false) RequestStatus status) {
        requireRole(role, "EMPLOYEE");
        return service.mine(employeeId, status);
    }

    @GetMapping("/{id}")
    public ProductRequestResponse get(@PathVariable Integer id,
                                      @RequestHeader("X-Authenticated-User-Id") Integer callerId,
                                      @RequestHeader("X-Authenticated-Role") String role) {
        // The service makes the second-level ownership check for employees; admins can
        // review any request, but customers and unauthenticated callers cannot view one.
        if (!"EMPLOYEE".equals(role) && !"ADMIN".equals(role)) throw new ForbiddenOperationException("Access denied");
        return service.get(id, callerId, role);
    }

    @GetMapping
    public List<ProductRequestResponse> all(@RequestHeader("X-Authenticated-Role") String role,
                                            @RequestParam(required = false) RequestStatus status,
                                            @RequestParam(required = false) Integer employeeId,
                                            @RequestParam(required = false) RequestAction action) {
        requireRole(role, "ADMIN");
        return service.findAll(status, employeeId, action);
    }

    @PatchMapping("/{id}/status")
    public ProductRequestResponse updateStatus(@PathVariable Integer id,
                                               @RequestHeader("X-Authenticated-User-Id") Integer adminId,
                                               @RequestHeader("X-Authenticated-Role") String role,
                                               @Valid @RequestBody UpdateRequestStatus request) {
        // Status changes perform approval/rejection workflow and may mutate ProductApp;
        // only an administrator may initiate that decision.
        requireRole(role, "ADMIN");
        return service.updateStatus(id, adminId, request);
    }

    private void requireRole(String actual, String expected) {
        if (!expected.equals(actual)) throw new ForbiddenOperationException("Access denied");
    }
}
