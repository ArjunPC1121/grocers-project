package com.oracle.employeeapp.api;

import com.oracle.employeeapp.entities.Employee;
import com.oracle.employeeapp.repositories.EmployeeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/grocers/api/employees")
public class EmployeeController {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final EmployeeRepository employees;
    private final RestTemplate restTemplate;

    public EmployeeController(EmployeeRepository employees, RestTemplate restTemplate) {
        this.employees = employees;
        this.restTemplate = restTemplate;
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

    private TicketSummary ticketAction(Integer ticketId, String action, Integer employeeId) {
        TicketSummary response = restTemplate.postForObject("http://localhost:8087/grocers/api/tickets/{ticketId}/" + action,
                new TicketActionRequest(employeeId), TicketSummary.class, ticketId);
        if (response == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ticket service returned no ticket");
        return response;
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
}
