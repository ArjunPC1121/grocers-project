package com.oracle.adminapp.dto;

import com.oracle.adminapp.entities.AdminRole;

public record AdminResponse(Integer id, String firstName, String lastName, String email, AdminRole role) {
}
