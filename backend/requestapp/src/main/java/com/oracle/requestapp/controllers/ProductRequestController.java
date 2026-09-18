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
        requireRole(role, "ADMIN");
        return service.updateStatus(id, adminId, request);
    }

    private void requireRole(String actual, String expected) {
        if (!expected.equals(actual)) throw new ForbiddenOperationException("Access denied");
    }
}
