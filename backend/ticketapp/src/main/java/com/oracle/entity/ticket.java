package com.oracle.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "tickets")
public class ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private int ticketId;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private static ticket status = ticket.status;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    protected ticket() { }

    public ticket(int userId, String description) {
        this.userId = userId;
        this.description = description;
    }

    public int  getTicketId() { return ticketId; }
    public int getUserId() { return userId; }
    public Long getEmployeeId() { return employeeId; }
    public ticket getStatus() { return status; }
    public String getDescription() { return description; }

    public void assignTo(Long employeeId) { this.employeeId = employeeId; this.updatedAt = Instant.now(); }
    public void setStatus(ticket status) { ticket.status = status; this.updatedAt = Instant.now(); }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}


