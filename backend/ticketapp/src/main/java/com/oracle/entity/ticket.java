package com.oracle.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;


@Entity
@Table(name = "tickets")
public class ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "employee_id")
    private Integer employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ticketstatus status = ticketstatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "locked_reason", nullable = false, length = 50)
    private LockedReason lockedReason;

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

    public ticket(Integer userId, LockedReason lockedReason) {
        this.userId = userId;
        this.lockedReason = lockedReason;
    }

    public Integer getTicketId() { return ticketId; }
    public Integer getUserId() { return userId; }
    public Integer getEmployeeId() { return employeeId; }
    public ticketstatus getStatus() { return status; }
    public LockedReason getLockedReason() { return lockedReason; }

    public void assignTo(Integer employeeId) { this.employeeId = employeeId; }
    public void resolve(Integer employeeId) { this.employeeId = employeeId; this.status = ticketstatus.RESOLVED; }
    public void reject(Integer employeeId) { this.employeeId = employeeId; this.status = ticketstatus.REJECTED; }

    @PreUpdate
    void markUpdated() { this.updatedAt = Instant.now(); }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}


