package com.oracle.orderapp.controllers;

import com.oracle.orderapp.dtos.CancelOrderRequest;
import com.oracle.orderapp.dtos.CreateOrderRequest;
import com.oracle.orderapp.dtos.UpdateOrderAddressRequest;
import com.oracle.orderapp.dtos.UpdateOrderStatusRequest;
import com.oracle.orderapp.entities.Order;
import com.oracle.orderapp.entities.OrderStatus;
import com.oracle.orderapp.services.abstractions.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/grocers/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(request));
    }

    @GetMapping
    public List<Order> getAll(@RequestHeader("X-Authenticated-Role") String role) {
        requireEmployeeOrAdmin(role);
        return orderService.getAll();
    }

    @GetMapping("/{orderId}")
    public Order getById(@PathVariable Integer orderId) {
        return orderService.getById(orderId);
    }

    @GetMapping("/customers/{customerId}")
    public List<Order> getByCustomer(@PathVariable Integer customerId) {
        return orderService.getByCustomerId(customerId);
    }
    @PutMapping("/{orderId}/address")
    public Order updateAddress(
            @PathVariable Integer orderId,
            @Valid @RequestBody UpdateOrderAddressRequest request) {

        return orderService.updateAddress(orderId, request);
    }

    @PostMapping("/{orderId}/checkout")
    public Order checkout(@PathVariable Integer orderId) {
        return orderService.checkout(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public Order cancel(
            @PathVariable Integer orderId,
            @Valid @RequestBody CancelOrderRequest request) {

        return orderService.cancel(orderId, request.reason());
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteCreatedOrder(@PathVariable Integer orderId) {
        orderService.deleteCreatedOrder(orderId);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{orderId}/status")
    public Order updateStatus(
            @PathVariable Integer orderId,
            @RequestHeader("X-Authenticated-User-Id") Integer employeeId,
            @RequestHeader("X-Authenticated-Role") String role,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        requireEmployeeOrAdmin(role);
        return orderService.updateStatus(orderId, request, employeeId);
    }
    @GetMapping("/status/{status}")
    public List<Order> getByStatus(
            @PathVariable OrderStatus status,
            @RequestHeader("X-Authenticated-Role") String role) {

        requireEmployeeOrAdmin(role);
        return orderService.getByStatus(status);
    }

    private void requireEmployeeOrAdmin(String role) {
        if (!"EMPLOYEE".equals(role) && !"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Employee or administrator access is required");
        }
    }
}
