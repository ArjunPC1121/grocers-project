package com.oracle.orderapp.controllers;

import com.oracle.orderapp.dtos.CancelOrderRequest;
import com.oracle.orderapp.dtos.CreateOrderRequest;
import com.oracle.orderapp.dtos.EmployeeCancelOrderRequest;
import com.oracle.orderapp.dtos.EmployeeOrderDetails;
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
    public List<Order> getAll() {
        return orderService.getAll();
    }

    /** Used by EmployeeApp to show fulfilment details without exposing them to customer order views. */
    @GetMapping("/employee-details")
    public List<EmployeeOrderDetails> getAllEmployeeDetails(
            @RequestHeader("X-Authenticated-User-Id") Integer employeeId) {
        return orderService.getAllEmployeeDetails(employeeId);
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
    @PostMapping("/{orderId}/employee-cancel")
    public Order cancelByEmployee(
            @PathVariable Integer orderId,
            @Valid @RequestBody EmployeeCancelOrderRequest request) {
        return orderService.cancelByEmployee(orderId, request.reason(), request.employeeId());
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteCreatedOrder(@PathVariable Integer orderId) {
        orderService.deleteCreatedOrder(orderId);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{orderId}/status")
    public Order updateStatus(
            @PathVariable Integer orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        return orderService.updateStatus(orderId, request);
    }
    @GetMapping("/status/{status}")
    public List<Order> getByStatus(
            @PathVariable OrderStatus status) {

        return orderService.getByStatus(status);
    }
}
