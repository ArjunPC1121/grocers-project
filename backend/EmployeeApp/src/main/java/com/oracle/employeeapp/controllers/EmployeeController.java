package com.oracle.employeeapp.controllers;

import com.oracle.employeeapp.dtos.ChangePasswordRequest;
import com.oracle.employeeapp.dtos.CreateEmployeeRequest;
import com.oracle.employeeapp.dtos.EmployeeSummary;
import com.oracle.employeeapp.dtos.InventoryRequest;
import com.oracle.employeeapp.dtos.ProductRequestSummary;
import com.oracle.employeeapp.dtos.TicketSummary;
import com.oracle.employeeapp.dtos.UpdateOrderStatusRequest;
import com.oracle.employeeapp.services.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/grocers/api/employees")
public class EmployeeController {
    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeSummary createEmployee(@RequestHeader("X-Authenticated-Role") String role,
                                          @Valid @RequestBody CreateEmployeeRequest request) {
        return service.createEmployee(role, request);
    }

    @PutMapping("/{employeeId}/password")
    public EmployeeSummary changePassword(@RequestHeader("X-Authenticated-User-Id") Integer authenticatedEmployeeId,
                                          @RequestHeader("X-Authenticated-Role") String role,
                                          @PathVariable Integer employeeId,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        return service.changePassword(authenticatedEmployeeId, role, employeeId, request);
    }

    @GetMapping("/tickets")
    public List<TicketSummary> openTickets(@RequestHeader("X-Authenticated-Role") String role) {
        return service.openTickets(role);
    }

    @PostMapping("/tickets/{ticketId}/resolve")
    public TicketSummary resolveTicket(@RequestHeader("X-Authenticated-User-Id") Integer employeeId,
                                       @RequestHeader("X-Authenticated-Role") String role,
                                       @PathVariable Integer ticketId) {
        return service.resolveTicket(employeeId, role, ticketId);
    }

    @PostMapping("/tickets/{ticketId}/reject")
    public TicketSummary rejectTicket(@RequestHeader("X-Authenticated-User-Id") Integer employeeId,
                                      @RequestHeader("X-Authenticated-Role") String role,
                                      @PathVariable Integer ticketId) {
        return service.rejectTicket(employeeId, role, ticketId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductRequestSummary createProductRequest(@RequestHeader("X-Authenticated-User-Id") Integer employeeId,
                                                      @RequestHeader("X-Authenticated-Role") String role,
                                                      @Valid @RequestBody InventoryRequest request) {
        return service.createProductRequest(employeeId, role, request);
    }

    @GetMapping("/requests")
    public List<ProductRequestSummary> myProductRequests(@RequestHeader("X-Authenticated-User-Id") Integer employeeId,
                                                          @RequestHeader("X-Authenticated-Role") String role) {
        return service.myProductRequests(employeeId, role);
    }

    @GetMapping("/orders")
    public List<Object> getAllOrders(@RequestHeader("X-Authenticated-User-Id") Integer employeeId,
                                     @RequestHeader("X-Authenticated-Role") String role) {
        return service.getAllOrders(employeeId, role);
    }

    @PatchMapping("/orders/{orderId}/status")
    public Object updateOrderStatus(@RequestHeader("X-Authenticated-User-Id") Integer employeeId,
                                    @RequestHeader("X-Authenticated-Role") String role,
                                    @PathVariable Integer orderId,
                                    @Valid @RequestBody UpdateOrderStatusRequest request) {
        return service.updateOrderStatus(employeeId, role, orderId, request);
    }
}
