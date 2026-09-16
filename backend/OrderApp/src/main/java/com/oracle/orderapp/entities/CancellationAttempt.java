package com.oracle.orderapp.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "cancellation_attempts")
public class CancellationAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Version private Long version;
    @Column(name = "operation_key", nullable = false, unique = true, length = 100) private String operationKey;
    @Column(name = "order_number", nullable = false, unique = true, length = 50) private String orderNumber;
    @Column(name = "employee_id", nullable = false) private Integer employeeId;
    @Column(name = "reason", nullable = false, length = 1000) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private CancellationStep step;
    @Column(name = "failure_code", length = 100) private String failureCode;
    @Column(name = "failure_message", length = 1000) private String failureMessage;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public static CancellationAttempt start(String key, String orderNumber, Integer employeeId, String reason) {
        CancellationAttempt attempt = new CancellationAttempt();
        attempt.operationKey = key; attempt.orderNumber = orderNumber;
        attempt.employeeId = employeeId; attempt.reason = reason; attempt.step = CancellationStep.STARTED;
        return attempt;
    }
    public void advanceTo(CancellationStep next) { step = next; }
    public void fail(String code, String message) { failureCode = code; failureMessage = message; step = CancellationStep.FAILED; }
    public void recordFailure(String code, String message) {
        failureCode = code;
        failureMessage = message == null ? null : message.substring(0, Math.min(message.length(), 1000));
    }
    public void clearFailure() { failureCode = null; failureMessage = null; }
    @PrePersist void created() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
}
