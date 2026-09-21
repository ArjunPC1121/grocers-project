package com.oracle.adminapp.services;

import com.oracle.adminapp.dto.AdminCreateRequest;
import com.oracle.adminapp.dto.AdminResponse;
import com.oracle.adminapp.dto.AdminUpdateRequest;
import com.oracle.adminapp.entities.Admin;
import com.oracle.adminapp.entities.AdminRole;
import com.oracle.adminapp.exceptions.ForbiddenOperationException;
import com.oracle.adminapp.exceptions.ResourceNotFoundException;
import com.oracle.adminapp.repositories.AdminRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminService {
    private final AdminRepository admins;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminService(AdminRepository admins) {
        this.admins = admins;
    }

    @Transactional(readOnly = true)
    public AdminResponse currentAdmin(Integer adminId) {
        return toResponse(requireAdmin(adminId));
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> normalAdmins(Integer actorId) {
        requireSuperAdmin(actorId);
        return admins.findAll().stream()
                .filter(admin -> admin.getRole() == AdminRole.ADMIN)
                .map(this::toResponse)
                .toList();
    }

    public AdminResponse createNormalAdmin(Integer actorId, AdminCreateRequest request) {
        requireSuperAdmin(actorId);
        if (admins.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new IllegalArgumentException("An admin already exists with this email address");
        }

        Admin admin = new Admin();
        admin.setFirstName(request.firstName());
        admin.setLastName(request.lastName());
        admin.setEmail(request.email().trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(request.password()));
        admin.setRole(AdminRole.ADMIN);
        return toResponse(admins.save(admin));
    }

    public AdminResponse updateNormalAdmin(Integer actorId, Integer targetId, AdminUpdateRequest request) {
        requireSuperAdmin(actorId);
        Admin target = requireNormalAdmin(targetId);
        admins.findByEmailIgnoreCase(request.email())
                .filter(existing -> !existing.getId().equals(targetId))
                .ifPresent(existing -> { throw new IllegalArgumentException("An admin already exists with this email address"); });

        target.setFirstName(request.firstName());
        target.setLastName(request.lastName());
        target.setEmail(request.email().trim().toLowerCase());
        return toResponse(target);
    }

    public void deleteNormalAdmin(Integer actorId, Integer targetId) {
        requireSuperAdmin(actorId);
        admins.delete(requireNormalAdmin(targetId));
    }

    public void requireSuperAdmin(Integer adminId) {
        Admin admin = requireAdmin(adminId);
        if (admin.getRole() != AdminRole.SUPER_ADMIN) {
            throw new ForbiddenOperationException("Only the Super Admin can manage admin accounts");
        }
    }

    private Admin requireAdmin(Integer id) {
        return admins.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated admin was not found"));
    }

    private Admin requireNormalAdmin(Integer id) {
        Admin admin = admins.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin was not found"));
        if (admin.getRole() != AdminRole.ADMIN) {
            throw new ForbiddenOperationException("The Super Admin account cannot be changed through this endpoint");
        }
        return admin;
    }

    private AdminResponse toResponse(Admin admin) {
        return new AdminResponse(admin.getId(), admin.getFirstName(), admin.getLastName(), admin.getEmail(), admin.getRole());
    }
}
