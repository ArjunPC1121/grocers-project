package com.oracle.orderapp.services;

import com.oracle.orderapp.dtos.OrderCancellationRequest;
import com.oracle.orderapp.dtos.OrderResponse;
import com.oracle.orderapp.dtos.OrderStatusUpdateRequest;
import com.oracle.orderapp.dtos.clients.EmployeeVerificationResponse;
import com.oracle.orderapp.dtos.clients.FundMutationRequest;
import com.oracle.orderapp.dtos.clients.FundMutationResponse;
import com.oracle.orderapp.dtos.clients.InventoryRestoreRequest;
import com.oracle.orderapp.dtos.clients.InventoryResponse;
import com.oracle.orderapp.entities.Order;
import com.oracle.orderapp.entities.OrderStatus;
import com.oracle.orderapp.entities.CancellationAttempt;
import com.oracle.orderapp.entities.CancellationStep;
import com.oracle.orderapp.exceptions.DownstreamContractException;
import com.oracle.orderapp.repositories.CancellationAttemptRepository;
import com.oracle.orderapp.repositories.OrderRepository;
import com.oracle.orderapp.services.abstractions.EmployeeClient;
import com.oracle.orderapp.services.abstractions.FundsClient;
import com.oracle.orderapp.services.abstractions.ProductClient;
import com.oracle.orderapp.services.implementations.OrderManagementServiceImpl;
import com.oracle.orderapp.services.implementations.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderManagementServiceImplTest {
    @Mock EmployeeClient employeeClient;
    @Mock ProductClient productClient;
    @Mock FundsClient fundsClient;
    @Mock OrderRepository orderRepository;
    @Mock CancellationAttemptRepository cancellationRepository;
    OrderManagementServiceImpl service;
    Order order;

    @BeforeEach
    void setUp() {
        service = new OrderManagementServiceImpl(employeeClient, productClient, fundsClient,
                orderRepository, cancellationRepository, new OrderMapper());
        order = new Order();
        order.setOrderNumber("ORD-1");
        order.setCustomerId(41);
        order.setCartId(25);
        order.setDeliveryAddress("12 Market Road");
        order.setTotalAmount(160.0d);
        order.setStatus(OrderStatus.PLACED);
        when(employeeClient.verify(7)).thenReturn(new EmployeeVerificationResponse(7, true));
        when(orderRepository.findByOrderNumber("ORD-1")).thenReturn(Optional.of(order));
        org.mockito.Mockito.lenient().when(orderRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(productClient.restore(any())).thenAnswer(invocation -> {
            InventoryRestoreRequest request = invocation.getArgument(0);
            return new InventoryResponse(request.orderNumber(), "RESTORED", List.of());
        });
        org.mockito.Mockito.lenient().when(fundsClient.refund(any())).thenAnswer(invocation -> {
            FundMutationRequest request = invocation.getArgument(0);
            return new FundMutationResponse(request.orderNumber(), request.userId(), request.amount(), 500.0d, "REFUND");
        });
    }

    @Test
    void employeeAdvancesOrderToNextStatus() {
        OrderResponse result = service.updateStatus(7, "ORD-1",
                new OrderStatusUpdateRequest(OrderStatus.SHIPPED));
        assertEquals(OrderStatus.SHIPPED, result.status());
        assertEquals(7, result.updatedByEmployeeId());
    }

    @Test
    void cancellationRestoresInventoryRefundsFundsAndCancelsOrder() {
        when(cancellationRepository.findByOperationKey("cancel-1")).thenReturn(Optional.empty());
        when(cancellationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = service.cancel(7, "cancel-1", "ORD-1",
                new OrderCancellationRequest("Damaged package"));

        assertEquals(OrderStatus.CANCELLED, result.status());
        assertEquals("Damaged package", result.cancellationReason());
        verify(productClient).restore(any());
        verify(fundsClient).refund(any());
    }

    @Test
    void cancellationRetryAfterInventoryRestoreDoesNotRestoreStockTwice() {
        CancellationAttempt attempt = CancellationAttempt.start("cancel-1", "ORD-1", 7, "Damaged package");
        attempt.advanceTo(CancellationStep.INVENTORY_RESTORED);
        when(cancellationRepository.findByOperationKey("cancel-1")).thenReturn(Optional.of(attempt));
        when(cancellationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = service.cancel(7, "cancel-1", "ORD-1",
                new OrderCancellationRequest("Damaged package"));

        assertEquals(OrderStatus.CANCELLED, result.status());
        verify(productClient, never()).restore(any());
        verify(fundsClient).refund(any());
    }

    @Test
    void cancellationRejectsNonFiniteRefundAcknowledgement() {
        when(cancellationRepository.findByOperationKey("cancel-1")).thenReturn(Optional.empty());
        when(cancellationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doAnswer(invocation -> {
            FundMutationRequest request = invocation.getArgument(0);
            return new FundMutationResponse(request.orderNumber(), request.userId(),
                    Double.NaN, 500.0d, "REFUND");
        }).when(fundsClient).refund(any());

        assertThrows(DownstreamContractException.class, () -> service.cancel(7, "cancel-1", "ORD-1",
                new OrderCancellationRequest("Damaged package")));

        assertEquals(OrderStatus.PLACED, order.getStatus());
    }
}
