package com.oracle.employeeapp.services;

import com.oracle.employeeapp.dtos.ChangePasswordRequest;
import com.oracle.employeeapp.dtos.CreateEmployeeRequest;
import com.oracle.employeeapp.dtos.EmployeeSummary;
import com.oracle.employeeapp.dtos.EmployeeOrderDetails;
import com.oracle.employeeapp.dtos.InventoryOperation;
import com.oracle.employeeapp.dtos.InventoryRequest;
import com.oracle.employeeapp.dtos.ProductRequestSummary;
import com.oracle.employeeapp.dtos.RequestAppPayload;
import com.oracle.employeeapp.dtos.TicketActionRequest;
import com.oracle.employeeapp.dtos.TicketSummary;
import com.oracle.employeeapp.dtos.TicketUserDetails;
import com.oracle.employeeapp.dtos.UpdateOrderStatusRequest;
import com.oracle.employeeapp.entities.Employee;
import com.oracle.employeeapp.repositories.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeService {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final EmployeeRepository employees;
    private final RestTemplate restTemplate;
    private final String requestAppUrl;
    private final String orderAppUrl;
    private final String internalRequestSecret;

    public EmployeeService(EmployeeRepository employees, RestTemplate restTemplate,
                           @Value("${services.requestapp-url}") String requestAppUrl,
                           @Value("${services.orderapp-url}") String orderAppUrl,
                           @Value("${app.gateway.internal-secret}") String internalRequestSecret) {
        this.employees = employees;
        this.restTemplate = restTemplate;
        this.requestAppUrl = requestAppUrl;
        this.orderAppUrl = orderAppUrl;
        this.internalRequestSecret = internalRequestSecret;
    }

    public EmployeeSummary createEmployee(String role, CreateEmployeeRequest request) {
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

    public List<EmployeeSummary> employees(String role) {
        requireAdmin(role);
        return employees.findAll().stream().map(this::summary).toList();
    }

    public EmployeeSummary employeeById(Integer employeeId) {
        return summary(employee(employeeId));
    }

    public EmployeeSummary changePassword(Integer authenticatedEmployeeId, String role, Integer employeeId,
                                          ChangePasswordRequest request) {
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

    public List<TicketSummary> openTickets(String role) {
        requireEmployee(role);
        TicketSummary[] tickets = restTemplate.getForObject("http://localhost:8087/grocers/api/tickets/open", TicketSummary[].class);
        return tickets == null ? List.of() : Arrays.stream(tickets).map(this::withCustomerDetails).toList();
    }

    public List<TicketSummary> ticketHistory(Integer employeeId, String role) {
        requireEmployee(role);
        TicketSummary[] tickets = restTemplate.getForObject(
                "http://localhost:8087/grocers/api/tickets/employee/{employeeId}/history", TicketSummary[].class, employeeId);
        return tickets == null ? List.of() : Arrays.stream(tickets).map(this::withCustomerDetails).toList();
    }

    public TicketSummary resolveTicket(Integer employeeId, String role, Integer ticketId) {
        requireEmployee(role);
        return withCustomerDetails(ticketAction(ticketId, "resolve", employeeId));
    }

    public TicketSummary rejectTicket(Integer employeeId, String role, Integer ticketId) {
        requireEmployee(role);
        return withCustomerDetails(ticketAction(ticketId, "reject", employeeId));
    }

    public ProductRequestSummary createProductRequest(Integer employeeId, String role, InventoryRequest request) {
        requireEmployee(role);
        RequestAppPayload payload = new RequestAppPayload(
                request.productId(),
                request.operation() == InventoryOperation.ADD ? "RESTOCK" : "DELETE",
                request.operation() == InventoryOperation.ADD ? request.quantity() : null,
                request.description());
        return requestApp(HttpMethod.POST, "", employeeId, payload, ProductRequestSummary.class);
    }

    public List<ProductRequestSummary> myProductRequests(Integer employeeId, String role) {
        requireEmployee(role);
        ProductRequestSummary[] response = requestApp(HttpMethod.GET, "/my", employeeId, null,
                ProductRequestSummary[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    public List<EmployeeOrderDetails> getAllOrders(Integer employeeId, String role) {
        requireEmployee(role);
        EmployeeOrderDetails[] orders = orderApp(HttpMethod.GET, "/employee-details", employeeId, role, null,
                EmployeeOrderDetails[].class);
        if (orders == null) {
            throw new RuntimeException("Could not retrieve orders from Orders service");
        }
        return Arrays.asList(orders);
    }

    public Object updateOrderStatus(Integer employeeId, String role, Integer orderId, UpdateOrderStatusRequest request) {
        requireEmployee(role);
        String status = request.status().trim().toUpperCase();
        if ("CANCELLED".equals(status)) {
            if (request.cancellationReason() == null || request.cancellationReason().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A cancellation reason is required");
            }
            return orderApp(HttpMethod.POST, "/" + orderId + "/cancel", employeeId, role,
                    Map.of("reason", request.cancellationReason().trim()), Object.class);
        }
        if (!List.of("SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported order status");
        }
        return orderApp(HttpMethod.PATCH, "/" + orderId + "/status", employeeId, role,
                Map.of("employeeId", employeeId, "status", status), Object.class);
    }

    private TicketSummary ticketAction(Integer ticketId, String action, Integer employeeId) {
        TicketSummary response = restTemplate.postForObject("http://localhost:8087/grocers/api/tickets/{ticketId}/" + action,
                new TicketActionRequest(employeeId), TicketSummary.class, ticketId);
        if (response == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ticket service returned no ticket");
        return response;
    }

    private TicketSummary withCustomerDetails(TicketSummary ticket) {
        TicketUserDetails customer = restTemplate.getForObject(
                "http://localhost:8080/grocers/api/users/{userId}/ticket-details", TicketUserDetails.class, ticket.userId());
        if (customer == null) return ticket;
        return new TicketSummary(ticket.ticketId(), ticket.userId(), ticket.employeeId(), ticket.status(),
                ticket.lockedReason(), ticket.createdAt(), ticket.updatedAt(), ticket.requestNote(),
                customer.firstName(), customer.lastName(), customer.email(), customer.accountLocked(),
                customer.failedLoginAttempts());
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
        if (!"EMPLOYEE".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee access is required");
        }
    }

    private void requireEmployeeAccess(Integer authenticatedEmployeeId, String role, Integer requestedEmployeeId) {
        if (!"ADMIN".equals(role) && (!"EMPLOYEE".equals(role) || !requestedEmployeeId.equals(authenticatedEmployeeId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may only access your own employee resources");
        }
    }

    private EmployeeSummary summary(Employee employee) {
        return new EmployeeSummary(employee.getId(), employee.getFirstName(), employee.getLastName(),
                employee.getEmail(), employee.getMustChangePassword());
    }
}
