package com.oracle.orderapp.services.implementations;

import com.oracle.orderapp.dtos.*;
import com.oracle.orderapp.dtos.clients.*;
import com.oracle.orderapp.entities.*;
import com.oracle.orderapp.exceptions.*;
import com.oracle.orderapp.repositories.*;
import com.oracle.orderapp.services.abstractions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class OrderManagementServiceImpl implements OrderManagementService {
    private final EmployeeClient employeeClient; private final ProductClient productClient;
    private final UserFundsClient fundsClient; private final OrderRepository orders;
    private final CancellationAttemptRepository cancellations; private final OrderMapper mapper;
    public OrderManagementServiceImpl(EmployeeClient employeeClient, ProductClient productClient, UserFundsClient fundsClient,
            OrderRepository orders, CancellationAttemptRepository cancellations, OrderMapper mapper) {
        this.employeeClient=employeeClient; this.productClient=productClient; this.fundsClient=fundsClient;
        this.orders=orders; this.cancellations=cancellations; this.mapper=mapper;
    }

    @Override @Transactional
    public OrderResponse updateStatus(Integer employeeId, String orderNumber, OrderStatusUpdateRequest request) {
        verifyEmployee(employeeId); Order order = find(orderNumber);
        OrderStatus expected = switch (order.getStatus()) {
            case PLACED -> OrderStatus.SHIPPED;
            case SHIPPED -> OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> OrderStatus.DELIVERED;
            default -> null;
        };
        if (expected == null || request.status() != expected)
            throw new InvalidStateException("INVALID_STATUS_TRANSITION", "Expected next status " + expected);
        order.setStatus(expected); order.setUpdatedByEmployeeId(employeeId);
        return mapper.toResponse(orders.save(order));
    }

    @Override
    public OrderResponse cancel(Integer employeeId, String key, String orderNumber, OrderCancellationRequest request) {
        if (key == null || key.isBlank())
            throw new OrderAppException("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key is required", HttpStatus.BAD_REQUEST);
        if (key.length() > 100)
            throw new OrderAppException("IDEMPOTENCY_KEY_TOO_LONG", "Idempotency-Key must not exceed 100 characters", HttpStatus.BAD_REQUEST);
        if (request == null || request.reason() == null || request.reason().isBlank())
            throw new OrderAppException("CANCELLATION_REASON_REQUIRED", "Cancellation reason is required", HttpStatus.BAD_REQUEST);
        String reason = request.reason().trim();
        verifyEmployee(employeeId); Order order = find(orderNumber);
        if (order.getStatus() == OrderStatus.DELIVERED)
            throw new InvalidStateException("ORDER_ALREADY_DELIVERED", "Delivered orders cannot be cancelled");
        var existing = cancellations.findByOperationKey(key);
        CancellationAttempt attempt;
        if (existing.isPresent()) {
            attempt = existing.get();
            if (!attempt.getOrderNumber().equals(orderNumber) || !attempt.getEmployeeId().equals(employeeId)
                    || !attempt.getReason().equals(reason))
                throw new InvalidStateException("IDEMPOTENCY_KEY_REUSED", "Cancellation key belongs to another order");
            if (attempt.getStep() == CancellationStep.COMPLETED) return mapper.toResponse(order);
        } else {
            if (order.getStatus() == OrderStatus.CANCELLED) return mapper.toResponse(order);
            if (cancellations.findByOrderNumber(orderNumber).isPresent())
                throw new InvalidStateException("CANCELLATION_ALREADY_STARTED",
                        "A cancellation has already started for this order; retry with its original key");
            try {
                attempt = cancellations.save(CancellationAttempt.start(key, orderNumber, employeeId, reason));
            } catch (DataIntegrityViolationException concurrentRequest) {
                throw new InvalidStateException("CANCELLATION_IN_PROGRESS", "Cancellation is already being processed");
            }
        }
        try {
            if (attempt.getStep().ordinal() < CancellationStep.INVENTORY_RESTORED.ordinal()) {
                InventoryResponse restored = productClient.restore(
                        new InventoryRestoreRequest(orderNumber + ":cancel-inventory", orderNumber));
                if (restored == null || !orderNumber.equals(restored.orderNumber())
                        || !"RESTORED".equals(restored.status()))
                    throw new DownstreamContractException("Products returned a contradictory restore response");
                attempt.advanceTo(CancellationStep.INVENTORY_RESTORED); attempt = cancellations.save(attempt);
            }
            if (attempt.getStep().ordinal() < CancellationStep.FUNDS_REFUNDED.ordinal()) {
                UserFundsMutationResponse refunded = fundsClient.refund(
                        new UserFundsMutationRequest(order.getUserId(), order.getTotalAmount()));
                if (refunded == null || !order.getUserId().equals(refunded.userId()) || refunded.amount() == null
                        || !Double.isFinite(refunded.amount())
                        || Math.abs(order.getTotalAmount() - refunded.amount()) > 0.001d)
                    throw new DownstreamContractException("User funds returned a contradictory refund response");
                attempt.advanceTo(CancellationStep.FUNDS_REFUNDED); attempt = cancellations.save(attempt);
            }
            if (attempt.getStep().ordinal() < CancellationStep.ORDER_CANCELLED.ordinal()) {
                order.setStatus(OrderStatus.CANCELLED); order.setUpdatedByEmployeeId(employeeId);
                order.setCancellationReason(reason); order = orders.save(order);
                attempt.advanceTo(CancellationStep.ORDER_CANCELLED); attempt = cancellations.save(attempt);
            }
            attempt.clearFailure();
            attempt.advanceTo(CancellationStep.COMPLETED); cancellations.save(attempt);
            return mapper.toResponse(order);
        } catch (RuntimeException failure) {
            String code = failure instanceof OrderAppException appFailure
                    ? appFailure.getCode() : "CANCELLATION_FAILED";
            attempt.recordFailure(code, failure.getMessage());
            cancellations.save(attempt);
            throw failure;
        }
    }

    private void verifyEmployee(Integer employeeId) {
        EmployeeVerificationResponse response = employeeClient.verify(employeeId);
        if (response == null || !response.valid() || !employeeId.equals(response.employeeId()))
            throw new AccessDeniedException("INVALID_EMPLOYEE", "Employee verification failed");
    }
    private Order find(String number) { return orders.findByOrderNumber(number)
            .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order " + number + " was not found")); }
}
