/**
 * Component role: Defines the HTTP boundary for this service. It accepts transport input, reads trusted gateway identity headers where required, and delegates business work to the service layer.
 *
 * Maintainer note: this file belongs to adminapp. See backend/adminapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.adminapp.controllers;

import com.oracle.adminapp.dto.AdminCreateRequest;
import com.oracle.adminapp.dto.AdminPasswordChangeRequest;
import com.oracle.adminapp.dto.AdminProfileUpdateRequest;
import com.oracle.adminapp.dto.AdminResponse;
import com.oracle.adminapp.dto.AdminUpdateRequest;
import com.oracle.adminapp.dto.DashboardResponse;
import com.oracle.adminapp.dto.EmployeeCreateRequest;
import com.oracle.adminapp.dto.ProductCommand;
import com.oracle.adminapp.dto.ReportPeriod;
import com.oracle.adminapp.dto.ReportResponse;
import com.oracle.adminapp.dto.UserCreateRequest;
import com.oracle.adminapp.dto.UserUpdateRequest;
import com.oracle.adminapp.entities.AdminNotification;
import com.oracle.adminapp.repositories.AdminNotificationRepository;
import com.oracle.adminapp.services.AdminService;
import com.oracle.adminapp.services.ExternalAdminOperationsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/grocers/api/admin")
public class AdminController {
    private final AdminNotificationRepository notificationRepository;
    private final AdminService adminService;
    private final ExternalAdminOperationsService operations;

    public AdminController(
            AdminService adminService,
            ExternalAdminOperationsService operations,
            AdminNotificationRepository notificationRepository
    ) {
        this.adminService = adminService;
        this.operations = operations;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/me")
    public AdminResponse me(@RequestHeader("X-Authenticated-User-Id") Integer adminId) {
        return adminService.currentAdmin(adminId);
    }

    @PatchMapping("/me")
    public AdminResponse updateMyProfile(@RequestHeader("X-Authenticated-User-Id") Integer adminId,
                                         @Valid @RequestBody AdminProfileUpdateRequest request) {
        return adminService.updateMyProfile(adminId, request);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(@RequestHeader("X-Authenticated-User-Id") Integer adminId,
                                                  @Valid @RequestBody AdminPasswordChangeRequest request) {
        adminService.changeMyPassword(adminId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return operations.dashboard();
    }

    // Only the seeded Super Admin can manage normal admin accounts.
    @GetMapping("/admins")
    public List<AdminResponse> admins(@RequestHeader("X-Authenticated-User-Id") Integer adminId) {
        return adminService.normalAdmins(adminId);
    }

    @PostMapping("/admins")
    public ResponseEntity<AdminResponse> createAdmin(@RequestHeader("X-Authenticated-User-Id") Integer adminId,
                                                       @Valid @RequestBody AdminCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createNormalAdmin(adminId, request));
    }

    @PutMapping("/admins/{id}")
    public AdminResponse updateAdmin(@RequestHeader("X-Authenticated-User-Id") Integer adminId,
                                     @PathVariable Integer id,
                                     @Valid @RequestBody AdminUpdateRequest request) {
        return adminService.updateNormalAdmin(adminId, id, request);
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<Void> deleteAdmin(@RequestHeader("X-Authenticated-User-Id") Integer adminId,
                                            @PathVariable Integer id) {
        adminService.deleteNormalAdmin(adminId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products")
    public List<Map<String, Object>> products() { return operations.products(); }

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(@Valid @RequestBody ProductCommand request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(operations.createProduct(request));
    }

    @PutMapping("/products/{id}")
    public Map<String, Object> updateProduct(@PathVariable Integer id, @Valid @RequestBody ProductCommand request) {
        return operations.updateProduct(id, request);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        operations.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/employees")
    public List<Map<String, Object>> employees() { return operations.employees(); }

    @PostMapping("/employees")
    public ResponseEntity<Map<String, Object>> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(operations.createEmployee(request));
    }

    @PostMapping("/employees/{id}/deactivate")
    public ResponseEntity<Void> deactivateEmployee(@PathVariable Integer id) {
        operations.deactivateEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/employees/{id}/activate")
    public ResponseEntity<Void> activateEmployee(@PathVariable Integer id) {
        operations.activateEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() { return operations.users(); }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(operations.createUser(request));
    }

    @PatchMapping("/users/{id}")
    public Map<String, Object> updateUser(@PathVariable Integer id, @Valid @RequestBody UserUpdateRequest request) {
        return operations.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        operations.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requests")
    public List<Map<String, Object>> requests() { return operations.requests(); }

    @PostMapping("/requests/{id}/approve")
    public Map<String, Object> approveRequest(@PathVariable Integer id,
                                               @RequestHeader("X-Authenticated-User-Id") Integer adminId) {
        return operations.approveRequest(id, adminId);
    }

    @PostMapping("/requests/{id}/reject")
    public Map<String, Object> rejectRequest(@PathVariable Integer id,
                                              @RequestHeader("X-Authenticated-User-Id") Integer adminId,
                                              @RequestBody Map<String, String> body) {
        String reason = body.get("rejectionReason");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("A rejection reason is required");
        return operations.rejectRequest(id, adminId, reason);
    }

    @GetMapping("/reports")
    public ReportResponse report(
            @RequestParam ReportPeriod period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) Integer customerId) {
        return operations.report(period, referenceDate, productId, customerId);
    }
    @GetMapping("/notifications")
    public List<AdminNotification> notifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    @PatchMapping("/notifications/{id}/read")
    public AdminNotification markNotificationAsRead(
            @PathVariable Integer id
    ) {
        AdminNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        notification.setRead(true);

        return notificationRepository.save(notification);
    }
}
