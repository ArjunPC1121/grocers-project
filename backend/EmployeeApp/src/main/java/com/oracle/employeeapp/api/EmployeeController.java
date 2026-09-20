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

@RestController
@RequestMapping("/grocers/api/employees")
public class EmployeeController {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final EmployeeRepository employees;

    public EmployeeController(EmployeeRepository employees) {
        this.employees = employees;
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

    private Employee employee(Integer id) {
        return employees.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid employee ID or password"));
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required");
        }
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
}
