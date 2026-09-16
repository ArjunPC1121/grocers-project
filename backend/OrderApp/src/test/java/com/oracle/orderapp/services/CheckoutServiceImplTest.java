package com.oracle.orderapp.services;

import com.oracle.orderapp.dtos.CheckoutRequest;
import com.oracle.orderapp.dtos.OrderResponse;
import com.oracle.orderapp.dtos.clients.CartItemResponse;
import com.oracle.orderapp.dtos.clients.CartResponse;
import com.oracle.orderapp.dtos.clients.CartTransitionRequest;
import com.oracle.orderapp.dtos.clients.FundMutationResponse;
import com.oracle.orderapp.dtos.clients.FundMutationRequest;
import com.oracle.orderapp.dtos.clients.InventoryItemResponse;
import com.oracle.orderapp.dtos.clients.InventoryResponse;
import com.oracle.orderapp.dtos.clients.InventoryDecrementRequest;
import com.oracle.orderapp.dtos.clients.UserVerificationResponse;
import com.oracle.orderapp.entities.CheckoutAttempt;
import com.oracle.orderapp.entities.CheckoutStep;
import com.oracle.orderapp.entities.Order;
import com.oracle.orderapp.entities.OrderStatus;
import com.oracle.orderapp.exceptions.DownstreamConflictException;
import com.oracle.orderapp.exceptions.DownstreamContractException;
import com.oracle.orderapp.exceptions.DownstreamUnavailableException;
import com.oracle.orderapp.exceptions.OrderAppException;
import com.oracle.orderapp.repositories.CheckoutAttemptRepository;
import com.oracle.orderapp.repositories.OrderRepository;
import com.oracle.orderapp.services.abstractions.CartClient;
import com.oracle.orderapp.services.abstractions.FundsClient;
import com.oracle.orderapp.services.abstractions.ProductClient;
import com.oracle.orderapp.services.abstractions.UserClient;
import com.oracle.orderapp.services.implementations.CheckoutServiceImpl;
import com.oracle.orderapp.services.implementations.OrderMapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import org.springframework.http.HttpStatus;
import org.mockito.ArgumentCaptor;

public class CheckoutServiceImplTest {
    UserClient userClient;
    CartClient cartClient;
    ProductClient productClient;
    FundsClient fundsClient;
    OrderRepository orderRepository;
    CheckoutAttemptRepository attemptRepository;
    CheckoutServiceImpl service;

    @BeforeMethod
    void setUp() {
        userClient = mock(UserClient.class);
        cartClient = mock(CartClient.class);
        productClient = mock(ProductClient.class);
        fundsClient = mock(FundsClient.class);
        orderRepository = mock(OrderRepository.class);
        attemptRepository = mock(CheckoutAttemptRepository.class);
        service = new CheckoutServiceImpl(userClient, cartClient, productClient, fundsClient,
                orderRepository, attemptRepository, new OrderMapper());
        when(attemptRepository.findByIdempotencyKey("checkout-1")).thenReturn(Optional.empty());
        when(attemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(productClient.restore(any())).thenAnswer(invocation -> {
            var request = (com.oracle.orderapp.dtos.clients.InventoryRestoreRequest) invocation.getArgument(0);
            return new InventoryResponse(request.orderNumber(), "RESTORED", List.of());
        });
        org.mockito.Mockito.lenient().when(fundsClient.refund(any())).thenAnswer(invocation -> {
            FundMutationRequest request = invocation.getArgument(0);
            return new FundMutationResponse(request.orderNumber(), request.userId(), request.amount(), 500.0d, "REFUND");
        });
        org.mockito.Mockito.lenient().when(cartClient.restore(any(), any())).thenAnswer(invocation -> {
            Integer cartId = invocation.getArgument(0);
            CartTransitionRequest request = invocation.getArgument(1);
            return new CartResponse(cartId, request.userId(), "ACTIVE", null, List.of());
        });
    }

    @Test
    void checkoutPersistsPlacedOrderFromDownstreamSnapshots() {
        when(userClient.verify(41)).thenReturn(new UserVerificationResponse(41, true,
                "user@example.com", "12 Market Road"));
        when(cartClient.get(25)).thenReturn(new CartResponse(25, 41, "ACTIVE", null,
                List.of(new CartItemResponse(10, 2))));
        arrangeProductSnapshot();
        arrangeSuccessfulDebit();
        arrangeSuccessfulCartCheckout();
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1);
            return order;
        });

        OrderResponse result = service.checkout(41, "checkout-1", new CheckoutRequest(25));

        assertEquals(OrderStatus.PLACED, result.status());
        assertEquals(160.0d, result.totalAmount(), 0.001d);
        assertEquals("Rice", result.items().get(0).productName());
        verify(cartClient).checkout(any(), any());
    }

    @Test
    void insufficientFundsRestoresInventoryAndDoesNotSaveOrder() {
        when(userClient.verify(41)).thenReturn(new UserVerificationResponse(41, true,
                "user@example.com", "12 Market Road"));
        when(cartClient.get(25)).thenReturn(new CartResponse(25, 41, "ACTIVE", null,
                List.of(new CartItemResponse(10, 2))));
        arrangeProductSnapshot();
        when(fundsClient.debit(any())).thenThrow(new DownstreamConflictException("INSUFFICIENT_FUNDS", "Insufficient funds"));

        expectThrows(DownstreamConflictException.class,
                () -> service.checkout(41, "checkout-1", new CheckoutRequest(25)));

        verify(productClient).restore(any());
        verify(fundsClient, never()).refund(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void orderSaveFailureRefundsTheExactDebitedAmountThenRestoresInventory() {
        arrangeVerifiedCartAndInventory();
        arrangeSuccessfulDebit();
        when(orderRepository.save(any())).thenThrow(new IllegalStateException("database unavailable"));

        expectThrows(IllegalStateException.class,
                () -> service.checkout(41, "checkout-1", new CheckoutRequest(25)));

        ArgumentCaptor<FundMutationRequest> refund = ArgumentCaptor.forClass(FundMutationRequest.class);
        verify(fundsClient).refund(refund.capture());
        assertEquals(160.0d, refund.getValue().amount(), 0.001d);
        verify(productClient).restore(any());
    }

    @Test
    void incompleteCompensationReturnsServiceUnavailableAndStillTriesAllReverseSteps() {
        arrangeVerifiedCartAndInventory();
        arrangeSuccessfulDebit();
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1);
            return order;
        });
        when(cartClient.checkout(any(), any()))
                .thenThrow(new DownstreamUnavailableException("cart", new RuntimeException("offline")));
        org.mockito.Mockito.doThrow(new DownstreamUnavailableException("cart", new RuntimeException("offline")))
                .when(cartClient).restore(any(), any());

        OrderAppException failure = expectThrows(OrderAppException.class,
                () -> service.checkout(41, "checkout-1", new CheckoutRequest(25)));

        assertEquals("COMPENSATION_INCOMPLETE", failure.getCode());
        verify(fundsClient).refund(any());
        verify(productClient).restore(any());
        verify(orderRepository, atLeastOnce()).save(org.mockito.ArgumentMatchers.argThat(
                order -> order.getStatus() == OrderStatus.CANCELLED));
    }

    @Test
    void invalidRefundAcknowledgementLeavesCompensationIncomplete() {
        arrangeVerifiedCartAndInventory();
        arrangeSuccessfulDebit();
        when(orderRepository.save(any())).thenThrow(new IllegalStateException("database unavailable"));
        org.mockito.Mockito.doAnswer(invocation -> {
            FundMutationRequest request = invocation.getArgument(0);
            return new FundMutationResponse(request.orderNumber(), request.userId(),
                    request.amount(), Double.NaN, "REFUND");
        }).when(fundsClient).refund(any());

        OrderAppException failure = expectThrows(OrderAppException.class,
                () -> service.checkout(41, "checkout-1", new CheckoutRequest(25)));

        assertEquals("COMPENSATION_INCOMPLETE", failure.getCode());
        verify(productClient).restore(any());
    }

    @Test
    void blankProductNameInInventorySnapshotIsRejectedAndInventoryIsRestored() {
        when(userClient.verify(41)).thenReturn(new UserVerificationResponse(41, true,
                "user@example.com", "12 Market Road"));
        when(cartClient.get(25)).thenReturn(new CartResponse(25, 41, "ACTIVE", null,
                List.of(new CartItemResponse(10, 2))));
        when(productClient.decrement(any())).thenAnswer(invocation -> {
            InventoryDecrementRequest request = invocation.getArgument(0);
            return new InventoryResponse(request.orderNumber(), "DECREMENTED",
                    List.of(new InventoryItemResponse(10, "   ", 2, 80.0d, 160.0d)));
        });

        expectThrows(DownstreamContractException.class,
                () -> service.checkout(41, "checkout-1", new CheckoutRequest(25)));

        verify(productClient).restore(any());
        verify(fundsClient, never()).debit(any());
    }

    @Test
    void resumesAnOrderSavedAttemptWithoutRepeatingInventoryOrFunds() {
        CheckoutAttempt attempt = CheckoutAttempt.start("checkout-1", "ORD-1", 41, 25);
        attempt.advanceTo(CheckoutStep.INVENTORY_DECREMENTED);
        attempt.advanceTo(CheckoutStep.FUNDS_DEBITED);
        attempt.advanceTo(CheckoutStep.ORDER_SAVED);
        when(attemptRepository.findByIdempotencyKey("checkout-1")).thenReturn(Optional.of(attempt));
        when(userClient.verify(41)).thenReturn(new UserVerificationResponse(41, true,
                "user@example.com", "12 Market Road"));
        when(cartClient.get(25)).thenReturn(new CartResponse(25, 41, "ACTIVE", null,
                List.of(new CartItemResponse(10, 2))));
        Order saved = new Order();
        saved.setOrderNumber("ORD-1");
        saved.setUserId(41);
        saved.setCartId(25);
        saved.setDeliveryAddress("12 Market Road");
        saved.setStatus(OrderStatus.PLACED);
        saved.setTotalAmount(160.0d);
        when(orderRepository.findByOrderNumber("ORD-1")).thenReturn(Optional.of(saved));
        arrangeSuccessfulCartCheckout();

        OrderResponse result = service.checkout(41, "checkout-1", new CheckoutRequest(25));

        assertEquals("ORD-1", result.orderNumber());
        verify(cartClient).checkout(any(), any());
        verify(productClient, never()).decrement(any());
        verify(fundsClient, never()).debit(any());
        assertEquals(CheckoutStep.COMPLETED, attempt.getStep());
    }

    @Test
    void failureBeforeCartCheckoutDoesNotRestoreAnUnchangedCart() {
        arrangeVerifiedCartAndInventory();
        arrangeSuccessfulDebit();
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1);
            return order;
        });
        when(attemptRepository.save(any())).thenAnswer(invocation -> {
            CheckoutAttempt attempt = invocation.getArgument(0);
            if (attempt.getStep() == CheckoutStep.ORDER_SAVED) {
                throw new IllegalStateException("attempt persistence failed");
            }
            return attempt;
        });

        expectThrows(IllegalStateException.class,
                () -> service.checkout(41, "checkout-1", new CheckoutRequest(25)));

        verify(cartClient, never()).checkout(any(), any());
        verify(cartClient, never()).restore(any(), any());
        verify(fundsClient).refund(any());
        verify(productClient).restore(any());
    }

    @Test
    void ambiguousDebitTimeoutIsRefundedAndReplayedWithTheOriginalStatus() {
        arrangeVerifiedCartAndInventory();
        when(fundsClient.debit(any())).thenThrow(
                new DownstreamUnavailableException("funds", new RuntimeException("read timed out")));

        OrderAppException initial = expectThrows(OrderAppException.class,
                () -> service.checkout(41, "checkout-1", new CheckoutRequest(25)));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, initial.getStatus());
        verify(fundsClient).refund(any());
        verify(productClient).restore(any());
        ArgumentCaptor<CheckoutAttempt> attempts = ArgumentCaptor.forClass(CheckoutAttempt.class);
        verify(attemptRepository, atLeastOnce()).save(attempts.capture());
        CheckoutAttempt terminal = attempts.getAllValues().get(attempts.getAllValues().size() - 1);
        when(attemptRepository.findByIdempotencyKey("checkout-1")).thenReturn(Optional.of(terminal));

        OrderAppException replay = expectThrows(OrderAppException.class,
                () -> service.checkout(41, "checkout-1", new CheckoutRequest(25)));

        assertEquals("DOWNSTREAM_UNAVAILABLE", replay.getCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, replay.getStatus());
    }

    @Test
    void retryReconcilesCartCheckedOutBeforeItsStepWasPersisted() {
        CheckoutAttempt attempt = CheckoutAttempt.start("checkout-1", "ORD-1", 41, 25);
        attempt.advanceTo(CheckoutStep.INVENTORY_DECREMENTED);
        attempt.advanceTo(CheckoutStep.FUNDS_DEBITED);
        attempt.advanceTo(CheckoutStep.ORDER_SAVED);
        attempt.beginCartMutation();
        when(attemptRepository.findByIdempotencyKey("checkout-1")).thenReturn(Optional.of(attempt));
        when(userClient.verify(41)).thenReturn(new UserVerificationResponse(41, true,
                "user@example.com", "12 Market Road"));
        when(cartClient.get(25)).thenReturn(new CartResponse(25, 41, "CHECKED_OUT", "ORD-1", List.of()));
        Order saved = new Order();
        saved.setOrderNumber("ORD-1");
        saved.setUserId(41);
        saved.setCartId(25);
        saved.setDeliveryAddress("12 Market Road");
        saved.setStatus(OrderStatus.PLACED);
        saved.setTotalAmount(160.0d);
        when(orderRepository.findByOrderNumber("ORD-1")).thenReturn(Optional.of(saved));

        OrderResponse result = service.checkout(41, "checkout-1", new CheckoutRequest(25));

        assertEquals("ORD-1", result.orderNumber());
        assertEquals(CheckoutStep.COMPLETED, attempt.getStep());
        verify(cartClient, never()).checkout(any(), any());
        verify(productClient, never()).decrement(any());
        verify(fundsClient, never()).debit(any());
    }

    private void arrangeVerifiedCartAndInventory() {
        when(userClient.verify(41)).thenReturn(new UserVerificationResponse(41, true,
                "user@example.com", "12 Market Road"));
        when(cartClient.get(25)).thenReturn(new CartResponse(25, 41, "ACTIVE", null,
                List.of(new CartItemResponse(10, 2))));
        arrangeProductSnapshot();
    }

    private void arrangeSuccessfulDebit() {
        when(fundsClient.debit(any())).thenAnswer(invocation -> {
            FundMutationRequest request = invocation.getArgument(0);
            return new FundMutationResponse(request.orderNumber(), request.userId(),
                    request.amount(), 340.0d, "DEBIT");
        });
    }

    private void arrangeProductSnapshot() {
        when(productClient.decrement(any())).thenAnswer(invocation -> {
            InventoryDecrementRequest request = invocation.getArgument(0);
            return new InventoryResponse(request.orderNumber(), "DECREMENTED",
                    List.of(new InventoryItemResponse(10, "Rice", 2, 80.0d, 160.0d)));
        });
    }

    private void arrangeSuccessfulCartCheckout() {
        when(cartClient.checkout(any(), any())).thenAnswer(invocation -> {
            Integer cartId = invocation.getArgument(0);
            CartTransitionRequest request = invocation.getArgument(1);
            return new CartResponse(cartId, request.userId(), "CHECKED_OUT", request.orderNumber(), List.of());
        });
    }
}
