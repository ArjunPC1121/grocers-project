/**
 * Component role: Maps a domain concept to persistent storage and contains the state that the service owns.
 *
 * Maintainer note: this file belongs to authapp. See backend/authapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.authapp.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
public class AdminLoginAccount {
    @Id
    @Column(name = "admin_id")
    private Integer id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    public Integer getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
