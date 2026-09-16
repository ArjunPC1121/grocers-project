package com.oracle.orderapp.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "checkout_attempts")
public class CheckoutAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Version private Long version;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100) private String idempotencyKey;
    @Column(name = "order_number", nullable = false, unique = true, length = 50) private String orderNumber;
    @Column(name = "user_id", nullable = false) private Integer userId;
    @Column(name = "cart_id", nullable = false) private Integer cartId;
    @Column(name = "order_id") private Integer orderId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private CheckoutStep step;
    @Column(name = "failure_code", length = 100) private String failureCode;
    @Column(name = "failure_message", length = 1000) private String failureMessage;
    @Column(name = "failure_status") private Integer failureStatus;
    @Column(name = "processing_token", length = 36) private String processingToken;
    @Column(name = "processing_expires_at") private LocalDateTime processingExpiresAt;
    @Column(name = "inventory_mutation_attempted", nullable = false) private boolean inventoryMutationAttempted;
    @Column(name = "funds_mutation_attempted", nullable = false) private boolean fundsMutationAttempted;
    @Column(name = "cart_mutation_attempted", nullable = false) private boolean cartMutationAttempted;
    @Column(name = "debited_amount") private Double debitedAmount;
    @Enumerated(EnumType.STRING) @Column(name = "compensation_from_step", length = 30)
    private CheckoutStep compensationFromStep;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public static CheckoutAttempt start(String key, String orderNumber, Integer userId, Integer cartId) {
        CheckoutAttempt attempt = new CheckoutAttempt();
        attempt.idempotencyKey = key;
        attempt.orderNumber = orderNumber;
        attempt.userId = userId;
        attempt.cartId = cartId;
        attempt.step = CheckoutStep.STARTED;
        return attempt;
    }

    public void advanceTo(CheckoutStep next) {
        if (step != null && next.ordinal() < step.ordinal()
                && next != CheckoutStep.COMPENSATING && next != CheckoutStep.FAILED) {
            throw new IllegalStateException("Checkout step cannot move backward");
        }
        step = next;
    }

    public void attachOrder(Integer orderId) { this.orderId = orderId; }
    public void claim(String token, LocalDateTime now, LocalDateTime expiresAt) {
        if (processingToken != null && processingExpiresAt != null && processingExpiresAt.isAfter(now)
                && !processingToken.equals(token)) {
            throw new IllegalStateException("Checkout is already being processed");
        }
        processingToken = token;
        processingExpiresAt = expiresAt;
    }
    public void releaseClaim() { processingToken = null; processingExpiresAt = null; }
    public void beginInventoryMutation() { inventoryMutationAttempted = true; }
    public void rejectInventoryMutation() { inventoryMutationAttempted = false; }
    public void beginFundsMutation(Double amount) { fundsMutationAttempted = true; debitedAmount = amount; }
    public void rejectFundsMutation() { fundsMutationAttempted = false; debitedAmount = null; }
    public void beginCartMutation() { cartMutationAttempted = true; }
    public void rejectCartMutation() { cartMutationAttempted = false; }
    public void beginCompensation() {
        if (step != CheckoutStep.COMPENSATING) compensationFromStep = step;
        step = CheckoutStep.COMPENSATING;
    }
    public void recordFailure(String code, String message, int status) {
        failureCode = code;
        failureMessage = message == null ? null : message.substring(0, Math.min(message.length(), 1000));
        failureStatus = status;
    }
    public void fail(String code, String message, int status) {
        recordFailure(code, message, status);
        step = CheckoutStep.FAILED;
    }

    @PrePersist void created() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
}
