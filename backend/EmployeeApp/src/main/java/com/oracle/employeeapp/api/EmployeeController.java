package com.oracle.employeeapp.api;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.oracle.employeeapp.entities.Employee;
import com.oracle.employeeapp.repositories.EmployeeRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/grocers/api/employees")
public class EmployeeController {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final EmployeeRepository employees;
    private final RestTemplate restTemplate;
    private final String requestAppUrl;
    private final String orderAppUrl;
    private final String internalRequestSecret;

    public EmployeeController(EmployeeRepository employees, RestTemplate restTemplate,
                              @Value("${services.requestapp-url}") String requestAppUrl,
                              @Value("${services.orderapp-url}") String orderAppUrl,
                              @Value("${app.gateway.internal-secret}") String internalRequestSecret) {
        this.employees = employees;
        this.restTemplate = restTemplate;
        this.requestAppUrl = requestAppUrl;
        this.orderAppUrl = orderAppUrl;
        this.internalRequestSecret = internalRequestSecret;
    }

    /** Called by an authenticated administrator through the gateway. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeSummary createEmployee(@RequestHeader("X-Authenticated-Role") String role,
                                          @Valid @RequestBody CreateEmployeeRequest request) {
        requireAdmin(role);
        if (employees.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An employee with this email already exists");
        }
        Employee employee = new Employee();
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setPassword(passwordEncoder.encode(request.defaultPassword()));
        employee.setMustChangePassword(true);
        return summary(employees.save(employee));
    }

    @PutMapping("/{employeeId}/password")
    public EmployeeSummary changePassword(@RequestHeader("X-Authenticated-User-Id") Integer authenticatedEmployeeId,
                                          @RequestHeader("X-Authenticated-Role") String role,
                                          @PathVariable Integer employeeId,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        requireEmployeeAccess(authenticatedEmployeeId, role, employeeId);
        Employee employee = employee(employeeId);
        if (!passwordEncoder.matches(request.currentPassword(), employee.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password and confirmation password do not match");
        }
        employee.setPassword(passwordEncoder.encode(request.newPassword()));
        employee.setMustChangePassword(false);
        return summary(employees.save(employee));
    }

    @GetMapping("/tickets")
    public List<TicketSummary> openTickets(@RequestHeader("X-Authenticated-Role") String role) {
        requireEmployee(role);
        TicketSummary[] tickets = restTemplate.getForObject("http://localhost:8087/grocers/api/tickets/open", TicketSummary[].class);
        return tickets == null ? List.of() : Arrays.asList(tickets);
    }

    @PostMapping("/tickets/{ticketId}/resolve")
    public TicketSummary resolveTicket(@RequestHeader("X-Authenticated-User-Id") Integer employeeId,
                                       @RequestHeader("X-Authenticated-Role") String role, @PathVariable Integer ticketId) {
        requireEmployee(role);
        return ticketAction(ticketId, "resolve", employeeId);
    }

    @PostMapping("/tickets/{ticketId}/reject")
    public TicketSummary rejectTicket(@RequestHeader("X-Authenticated-User-Id") Integer employeeId,
                                      @RequestHeader("X-Authenticated-Role") String role, @PathVariable Integer ticketId) {
        requireEmployee(role);
        return ticketAction(ticketId, "reject", employeeId);
    }

    /**
     * Creates a pending inventory request for an administrator to review.
     * ADD increases the selected product's stock; REMOVE requests deletion of
     * the selected product, which is the removal operation RequestApp supports.
     */
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductRequestSummary createProductRequest(
            @RequestHeader("X-Authenticated-User-Id") Integer employeeId,
            @RequestHeader("X-Authenticated-Role") String role,
            @Valid @RequestBody InventoryRequest request) {
        requireEmployee(role);

        RequestAppPayload payload = new RequestAppPayload(
                request.productId(),
                request.operation() == InventoryOperation.ADD ? "RESTOCK" : "DELETE",
                request.operation() == InventoryOperation.ADD ? request.quantity() : null,
                request.description());
        return requestApp(HttpMethod.POST, "", employeeId, payload, ProductRequestSummary.class);
    }

    /** Allows an employee to see the status and any rejection reason for their own requests. */
    @GetMapping("/requests")
    public List<ProductRequestSummary> myProductRequests(
            @RequestHeader("X-Authenticated-User-Id") Integer employeeId,
            @RequestHeader("X-Authenticated-Role") String role) {
        requireEmployee(role);
        ProductRequestSummary[] response = requestApp(HttpMethod.GET, "/my", employeeId, null,
                ProductRequestSummary[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    /** Returns all orders for the employee order-management screen. */
    // @GetMapping("/orders")
    // public List<Object> orders(@RequestHeader("X-Authenticated-User-Id") Integer employeeId,
    //                            @RequestHeader("X-Authenticated-Role") String role) {
    //     requireEmployee(role);
    //     Object[] response = orderApp(HttpMethod.GET, "", employeeId, role, null, Object[].class);
    //     return response == null ? List.of() : Arrays.asList(response);
    // }



@GetMapping("/orders")
public List<Object> getAllOrders(
        @RequestHeader("X-Authenticated-User-Id") Integer employeeId,
        @RequestHeader("X-Authenticated-Role") String role) {

    requireEmployee(role);

    Object[] orders = orderApp(
            HttpMethod.GET,
            "", // orderservice.url already ends with /grocers/api/orders
            employeeId,
            role,
            null,
            Object[].class
    );

    if (orders == null) {
        throw new RuntimeException("Could not retrieve orders from Orders service");
    }

    return Arrays.asList(orders);
}
    /** Advances delivery status or cancels an order on behalf of the authenticated employee. */
    @PatchMapping("/orders/{orderId}/status")
    public Object updateOrderStatus(@RequestHeader("X-Authenticated-User-Id") Integer employeeId,
                                    @RequestHeader("X-Authenticated-Role") String role,
                                    @PathVariable Integer orderId,
                                    @Valid @RequestBody UpdateOrderStatusRequest request) {
        requireEmployee(role);
        return orderApp(HttpMethod.PATCH, "/" + orderId + "/status", employeeId, role, request, Object.class);
    }

    private TicketSummary ticketAction(Integer ticketId, String action, Integer employeeId) {
        TicketSummary response = restTemplate.postForObject("http://localhost:8087/grocers/api/tickets/{ticketId}/" + action,
                new TicketActionRequest(employeeId), TicketSummary.class, ticketId);
        if (response == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ticket service returned no ticket");
        return response;
    }

    private <T> T requestApp(HttpMethod method, String path, Integer employeeId, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Authenticated-User-Id", employeeId.toString());
        headers.set("X-Authenticated-Role", "EMPLOYEE");
        try {
            ResponseEntity<T> response = restTemplate.exchange(requestAppUrl + path, method,
                    new HttpEntity<>(body, headers), responseType);
            return response.getBody();
        } catch (HttpStatusCodeException exception) {
            throw new ResponseStatusException(HttpStatus.valueOf(exception.getStatusCode().value()),
                    "Request service rejected the request", exception);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Request service is unavailable", exception);
        }
    }

    private <T> T orderApp(HttpMethod method, String path, Integer employeeId, String role, Object body,
                           Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Gateway-Request", internalRequestSecret);
        headers.set("X-Authenticated-User-Id", employeeId.toString());
        headers.set("X-Authenticated-Role", role);
        try {
            return restTemplate.exchange(orderAppUrl + path, method, new HttpEntity<>(body, headers), responseType)
                    .getBody();
        } catch (HttpStatusCodeException exception) {
            throw new ResponseStatusException(HttpStatus.valueOf(exception.getStatusCode().value()),
                    "Order service rejected the request", exception);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Order service is unavailable", exception);
        }
    }

    private Employee employee(Integer id) {
        return employees.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid employee ID or password"));
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required");
        }
    }

    private void requireEmployee(String role) {
        if (!"EMPLOYEE".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee access is required");
    }

    private void requireEmployeeAccess(Integer authenticatedEmployeeId, String role, Integer requestedEmployeeId) {
        if (!"ADMIN".equals(role) && (!("EMPLOYEE".equals(role)) || !requestedEmployeeId.equals(authenticatedEmployeeId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may only access your own employee resources");
        }
    }

    private EmployeeSummary summary(Employee employee) {
        return new EmployeeSummary(employee.getId(), employee.getFirstName(), employee.getLastName(),
                employee.getEmail(), employee.getMustChangePassword());
    }

    public record CreateEmployeeRequest(@NotBlank String firstName,
                                        @NotBlank String lastName,
                                        @NotBlank String email,
                                        @NotBlank String defaultPassword) { }
    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank String newPassword,
                                        @NotBlank String confirmPassword) { }
    public record EmployeeSummary(Integer id, String firstName, String lastName,
                                  String email, Boolean mustChangePassword) { }
    public record TicketActionRequest(Integer employeeId) { }
    public record TicketSummary(Integer ticketId, Integer userId, Integer employeeId, String status,
                                String lockedReason, java.time.Instant createdAt, java.time.Instant updatedAt) { }
    public enum InventoryOperation { ADD, REMOVE }
    public record InventoryRequest(@NotNull Integer productId,
                                   @NotNull InventoryOperation operation,
                                   @NotNull @Positive Integer quantity,
                                   String description) { }
    private record RequestAppPayload(Integer productId, String action, Integer quantity, String description) { }
    public record ProductRequestSummary(Integer requestId, Integer employeeId, Integer productId,
                                        String action, String status, String description, Integer quantity,
                                        String rejectionReason, Integer reviewedByAdminId) { }
    public record UpdateOrderStatusRequest(@NotBlank String status, String cancellationReason) { }
}
