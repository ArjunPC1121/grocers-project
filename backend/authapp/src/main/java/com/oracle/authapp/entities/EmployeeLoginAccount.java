package com.oracle.authapp.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees")
public class EmployeeLoginAccount {
    @Id
    private Integer id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(nullable = false)
    private String status;

    public Integer getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public String getStatus() { return status; }
}
