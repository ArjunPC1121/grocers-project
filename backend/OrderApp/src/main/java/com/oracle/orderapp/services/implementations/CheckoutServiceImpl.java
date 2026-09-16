package com.oracle.orderapp.services.implementations;

import com.oracle.orderapp.dtos.CheckoutRequest;
import com.oracle.orderapp.dtos.OrderResponse;
import com.oracle.orderapp.dtos.clients.*;
import com.oracle.orderapp.entities.*;
import com.oracle.orderapp.exceptions.*;
import com.oracle.orderapp.repositories.CheckoutAttemptRepository;
import com.oracle.orderapp.repositories.OrderRepository;
import com.oracle.orderapp.services.abstractions.*;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class CheckoutServiceImpl implements CheckoutService {
    private final UserClient userClient;
    private final CartClient cartClient;
    private final ProductClient productClient;
    private final FundsClient fundsClient;
    private final OrderRepository orderRepository;
    private final CheckoutAttemptRepository attemptRepository;
    private final OrderMapper mapper;
    private final Clock clock;

    @Autowired
    public CheckoutServiceImpl(UserClient userClient, CartClient cartClient, ProductClient productClient,
            FundsClient fundsClient, OrderRepository orderRepository, CheckoutAttemptRepository attemptRepository,
            OrderMapper mapper) {
        this(userClient, cartClient, productClient, fundsClient, orderRepository, attemptRepository, mapper, Clock.systemUTC());
    }

    CheckoutServiceImpl(UserClient userClient, CartClient cartClient, ProductClient productClient,
            FundsClient fundsClient, OrderRepository orderRepository, CheckoutAttemptRepository attemptRepository,
            OrderMapper mapper, Clock clock) {
        this.userClient = userClient; this.cartClient = cartClient; this.productClient = productClient;
        this.fundsClient = fundsClient; this.orderRepository = orderRepository;
        this.attemptRepository = attemptRepository; this.mapper = mapper; this.clock = clock;
    }

    @Override
    public OrderResponse checkout(Integer userId, String idempotencyKey, CheckoutRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new OrderAppException("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key is required", org.springframework.http.HttpStatus.BAD_REQUEST);
        if (idempotencyKey.length() > 100)
            throw new OrderAppException("IDEMPOTENCY_KEY_TOO_LONG", "Idempotency-Key must not exceed 100 characters", HttpStatus.BAD_REQUEST);
        if (request == null || request.cartId() == null)
            throw new OrderAppException("CART_ID_REQUIRED", "cartId is required", HttpStatus.BAD_REQUEST);
        var existing = attemptRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            validateAttemptOwner(existing.get(), userId, request.cartId());
            if (existing.get().getStep() == CheckoutStep.COMPLETED) return completed(existing.get());
            if (existing.get().getStep() == CheckoutStep.COMPENSATED || existing.get().getStep() == CheckoutStep.FAILED)
                throw terminalFailure(existing.get());
        }

        UserVerificationResponse user = userClient.verify(userId);
        if (user == null || !user.valid() || !userId.equals(user.userId())
                || user.deliveryAddress() == null || user.deliveryAddress().isBlank())
            throw new AccessDeniedException("INVALID_USER", "User verification failed");
        CartResponse cart = cartClient.get(request.cartId());
        if (existing.isPresent() && existing.get().getStep() == CheckoutStep.COMPENSATING) {
            validateCartIdentity(userId, request.cartId(), cart);
            CheckoutAttempt compensating = claim(existing.get());
            Order saved = orderRepository.findByOrderNumber(compensating.getOrderNumber()).orElse(null);
            compensate(compensating, saved, userId, cart.id());
            throw terminalFailure(compensating);
        }
        if (existing.isPresent() && (existing.get().getStep() == CheckoutStep.ORDER_SAVED
                || existing.get().getStep() == CheckoutStep.CART_CHECKED_OUT)) {
            validateCartIdentity(userId, request.cartId(), cart);
            return resumeSavedAttempt(claim(existing.get()), userId, cart);
        }
        validateCart(userId, request.cartId(), cart);

        CheckoutAttempt attempt = existing.orElseGet(() -> createAttempt(idempotencyKey, userId, cart.id()));
        attempt = claim(attempt);
        String orderNumber = attempt.getOrderNumber();
        Order order = null;
        try {
            attempt.beginInventoryMutation();
            try { attempt = attemptRepository.save(attempt); }
            catch (RuntimeException markerFailure) { attempt.rejectInventoryMutation(); throw markerFailure; }
            InventoryResponse inventory;
            try {
                inventory = productClient.decrement(new InventoryDecrementRequest(
                        orderNumber + ":inventory-decrement", orderNumber,
                        cart.items().stream().map(i -> new InventoryItemRequest(i.productId(), i.quantity())).toList()));
            } catch (DownstreamConflictException rejected) {
                attempt.rejectInventoryMutation();
                throw rejected;
            }
            if (attempt.getStep().ordinal() < CheckoutStep.INVENTORY_DECREMENTED.ordinal())
                attempt = mark(attempt, CheckoutStep.INVENTORY_DECREMENTED);
            validateInventory(orderNumber, cart, inventory);
            double total = OrderMapper.round2(inventory.items().stream().mapToDouble(InventoryItemResponse::subtotal).sum());
            attempt.beginFundsMutation(total);
            try { attempt = attemptRepository.save(attempt); }
            catch (RuntimeException markerFailure) { attempt.rejectFundsMutation(); throw markerFailure; }
            try {
                FundMutationResponse debit = fundsClient.debit(new FundMutationRequest(
                        orderNumber + ":funds-debit", orderNumber, userId, total));
                validateDebit(orderNumber, userId, total, debit);
            } catch (DownstreamConflictException rejected) {
                attempt.rejectFundsMutation();
                throw rejected;
            }
            if (attempt.getStep().ordinal() < CheckoutStep.FUNDS_DEBITED.ordinal())
                attempt = mark(attempt, CheckoutStep.FUNDS_DEBITED);
            order = orderRepository.save(mapper.fromCheckout(orderNumber, user, cart, inventory, total));
            attempt.attachOrder(order.getId()); attempt = mark(attempt, CheckoutStep.ORDER_SAVED);
            attempt.beginCartMutation();
            try { attempt = attemptRepository.save(attempt); }
            catch (RuntimeException markerFailure) { attempt.rejectCartMutation(); throw markerFailure; }
            try {
                CartResponse checkedOut = cartClient.checkout(cart.id(),
                        new CartTransitionRequest(orderNumber + ":cart-checkout", orderNumber, userId));
                validateCheckedOutCart(cart.id(), userId, orderNumber, checkedOut);
            } catch (DownstreamConflictException rejected) {
                attempt.rejectCartMutation();
                throw rejected;
            }
            attempt = mark(attempt, CheckoutStep.CART_CHECKED_OUT); attempt.releaseClaim(); mark(attempt, CheckoutStep.COMPLETED);
            return mapper.toResponse(order);
        } catch (RuntimeException failure) {
            int failureStatus = failure instanceof OrderAppException appFailure
                    ? appFailure.getStatus().value() : HttpStatus.INTERNAL_SERVER_ERROR.value();
            attempt.recordFailure(failure instanceof OrderAppException appFailure ? appFailure.getCode() : "CHECKOUT_FAILED",
                    failure.getMessage(), failureStatus);
            try { attempt = attemptRepository.save(attempt); } catch (RuntimeException ignored) { }
            boolean complete = compensate(attempt, order, userId, cart.id());
            if (!complete) {
                throw new OrderAppException("COMPENSATION_INCOMPLETE",
                        "Checkout failed and one or more compensation operations did not complete for " + orderNumber,
                        HttpStatus.SERVICE_UNAVAILABLE);
            }
            throw failure;
        }
    }

    private OrderResponse resumeSavedAttempt(CheckoutAttempt attempt, Integer userId, CartResponse cart) {
        String orderNumber = attempt.getOrderNumber();
        Order saved = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Saved checkout order was not found"));
        try {
            if (attempt.getStep() == CheckoutStep.ORDER_SAVED) {
                if (attempt.isCartMutationAttempted() && "CHECKED_OUT".equals(cart.status())
                        && orderNumber.equals(cart.checkedOutOrderNumber())) {
                    attempt = mark(attempt, CheckoutStep.CART_CHECKED_OUT);
                } else if ("ACTIVE".equals(cart.status())) {
                    attempt.beginCartMutation();
                    try { attempt = attemptRepository.save(attempt); }
                    catch (RuntimeException markerFailure) { attempt.rejectCartMutation(); throw markerFailure; }
                    CartResponse checkedOut = cartClient.checkout(cart.id(),
                            new CartTransitionRequest(orderNumber + ":cart-checkout", orderNumber, userId));
                    validateCheckedOutCart(cart.id(), userId, orderNumber, checkedOut);
                    attempt = mark(attempt, CheckoutStep.CART_CHECKED_OUT);
                } else {
                    throw new DownstreamContractException("Cart state does not match the saved checkout attempt");
                }
            }
            attempt.releaseClaim(); mark(attempt, CheckoutStep.COMPLETED);
            return mapper.toResponse(saved);
        } catch (RuntimeException failure) {
            int status = failure instanceof OrderAppException appFailure
                    ? appFailure.getStatus().value() : HttpStatus.INTERNAL_SERVER_ERROR.value();
            attempt.recordFailure(failure instanceof OrderAppException appFailure
                    ? appFailure.getCode() : "CHECKOUT_FAILED", failure.getMessage(), status);
            try { attempt = attemptRepository.save(attempt); } catch (RuntimeException ignored) { }
            boolean complete = compensate(attempt, saved, userId, cart.id());
            if (!complete) throw new OrderAppException("COMPENSATION_INCOMPLETE",
                    "Checkout recovery compensation did not complete for " + orderNumber,
                    HttpStatus.SERVICE_UNAVAILABLE);
            throw failure;
        }
    }

    private void validateAttemptOwner(CheckoutAttempt attempt, Integer userId, Integer cartId) {
        if (!attempt.getUserId().equals(userId) || !attempt.getCartId().equals(cartId))
            throw new InvalidStateException("IDEMPOTENCY_KEY_REUSED", "Idempotency key belongs to another checkout");
    }

    private OrderResponse completed(CheckoutAttempt attempt) {
        return orderRepository.findByOrderNumber(attempt.getOrderNumber()).map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Completed order was not found"));
    }

    private OrderAppException terminalFailure(CheckoutAttempt attempt) {
        String code = attempt.getFailureCode() == null ? "CHECKOUT_ALREADY_PROCESSED" : attempt.getFailureCode();
        String message = attempt.getFailureMessage() == null ? "Checkout is " + attempt.getStep() : attempt.getFailureMessage();
        HttpStatus status = attempt.getFailureStatus() == null ? HttpStatus.CONFLICT
                : HttpStatus.valueOf(attempt.getFailureStatus());
        return new OrderAppException(code, message, status);
    }

    private CheckoutAttempt claim(CheckoutAttempt attempt) {
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            attempt.claim(UUID.randomUUID().toString(), now, now.plusMinutes(2));
            return attemptRepository.saveAndFlush(attempt);
        } catch (IllegalStateException | OptimisticLockingFailureException failure) {
            throw new InvalidStateException("CHECKOUT_IN_PROGRESS", "Checkout is already being processed");
        }
    }

    private CheckoutAttempt createAttempt(String key, Integer userId, Integer cartId) {
        try {
            return attemptRepository.save(CheckoutAttempt.start(key, newOrderNumber(), userId, cartId));
        } catch (DataIntegrityViolationException concurrentRequest) {
            throw new InvalidStateException("CHECKOUT_IN_PROGRESS", "Checkout is already being processed");
        }
    }

    private void validateCart(Integer userId, Integer expectedCartId, CartResponse cart) {
        validateCartIdentity(userId, expectedCartId, cart);
        if (!"ACTIVE".equals(cart.status())) throw new InvalidStateException("CART_NOT_ACTIVE", "Cart is not active");
        if (cart.items() == null || cart.items().isEmpty())
            throw new OrderAppException("EMPTY_CART", "Cart has no items", org.springframework.http.HttpStatus.BAD_REQUEST);
        Set<Integer> ids = new HashSet<>();
        for (CartItemResponse item : cart.items()) {
            if (item.productId() == null || item.quantity() == null || item.quantity() < 1 || !ids.add(item.productId()))
                throw new OrderAppException("INVALID_CART_ITEMS", "Cart items are invalid", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }

    private void validateCartIdentity(Integer userId, Integer expectedCartId, CartResponse cart) {
        if (cart == null) throw new NotFoundException("CART_NOT_FOUND", "Cart was not found");
        if (!expectedCartId.equals(cart.id())) throw new DownstreamContractException("Cart returned a different cart id");
        if (!userId.equals(cart.userId())) throw new AccessDeniedException("CART_OWNERSHIP_MISMATCH", "Cart belongs to another user");
    }

    private void validateInventory(String orderNumber, CartResponse cart, InventoryResponse inventory) {
        if (inventory == null || !orderNumber.equals(inventory.orderNumber())
                || !"DECREMENTED".equals(inventory.status())
                || inventory.items() == null || inventory.items().size() != cart.items().size())
            throw new DownstreamContractException("Products returned incomplete inventory snapshots");
        java.util.Map<Integer, Integer> expectedItems = cart.items().stream().collect(
                java.util.stream.Collectors.toMap(CartItemResponse::productId, CartItemResponse::quantity));
        Set<Integer> returnedIds = new HashSet<>();
        for (InventoryItemResponse item : inventory.items()) {
            if (item.productId() == null || item.productName() == null || item.productName().isBlank()
                    || item.quantity() == null
                    || item.unitPrice() == null || item.subtotal() == null || item.unitPrice() < 0.0d
                    || !Double.isFinite(item.unitPrice()) || !Double.isFinite(item.subtotal())
                    || !returnedIds.add(item.productId())
                    || !item.quantity().equals(expectedItems.get(item.productId())))
                throw new DownstreamContractException("Products returned a malformed inventory snapshot");
            double expected = OrderMapper.round2(item.unitPrice() * item.quantity());
            if (Math.abs(expected - item.subtotal()) > 0.001d)
                throw new DownstreamContractException("Products returned an inconsistent subtotal");
        }
    }

    private void validateDebit(String orderNumber, Integer userId, double total, FundMutationResponse debit) {
        if (debit == null || !orderNumber.equals(debit.orderNumber()) || !userId.equals(debit.userId())
                || debit.amount() == null || !Double.isFinite(debit.amount())
                || debit.remainingBalance() == null || !Double.isFinite(debit.remainingBalance())
                || Math.abs(total - debit.amount()) > 0.001d || !"DEBIT".equals(debit.type())) {
            throw new DownstreamContractException("Funds returned a contradictory debit response");
        }
    }

    private void validateCheckedOutCart(Integer cartId, Integer userId, String orderNumber, CartResponse cart) {
        if (cart == null || !cartId.equals(cart.id()) || !userId.equals(cart.userId())
                || !"CHECKED_OUT".equals(cart.status())
                || !orderNumber.equals(cart.checkedOutOrderNumber())) {
            throw new DownstreamContractException("Cart returned a contradictory checkout response");
        }
    }

    private boolean compensate(CheckoutAttempt attempt, Order order, Integer userId, Integer cartId) {
        boolean complete = true;
        attempt.beginCompensation();
        try { attempt = attemptRepository.save(attempt); } catch (RuntimeException ignored) { complete = false; }
        if (attempt.isCartMutationAttempted()) {
            try {
                CartResponse restored = cartClient.restore(cartId, new CartTransitionRequest(
                        attempt.getOrderNumber() + ":cart-restore", attempt.getOrderNumber(), userId));
                if (restored == null || !cartId.equals(restored.id()) || !userId.equals(restored.userId())
                        || !"ACTIVE".equals(restored.status())) throw new DownstreamContractException("Cart restore was not acknowledged");
            }
            catch (RuntimeException ignored) { complete = false; }
        }
        if (attempt.isFundsMutationAttempted()) {
            try {
                FundMutationResponse refunded = fundsClient.refund(new FundMutationRequest(
                        attempt.getOrderNumber() + ":funds-refund", attempt.getOrderNumber(), userId, attempt.getDebitedAmount()));
                if (refunded == null || !attempt.getOrderNumber().equals(refunded.orderNumber())
                        || !userId.equals(refunded.userId()) || refunded.amount() == null
                        || !Double.isFinite(refunded.amount()) || refunded.remainingBalance() == null
                        || !Double.isFinite(refunded.remainingBalance())
                        || Math.abs(attempt.getDebitedAmount() - refunded.amount()) > 0.001d
                        || !"REFUND".equals(refunded.type())) throw new DownstreamContractException("Funds refund was not acknowledged");
            }
            catch (RuntimeException ignored) { complete = false; }
        }
        if (attempt.isInventoryMutationAttempted()) {
            try {
                InventoryResponse restored = productClient.restore(new InventoryRestoreRequest(
                        attempt.getOrderNumber() + ":inventory-restore", attempt.getOrderNumber()));
                if (restored == null || !attempt.getOrderNumber().equals(restored.orderNumber())
                        || !"RESTORED".equals(restored.status())) throw new DownstreamContractException("Inventory restore was not acknowledged");
            }
            catch (RuntimeException ignored) { complete = false; }
        }
        if (order != null) {
            order.setStatus(OrderStatus.CANCELLED); order.setCancellationReason("Checkout failed and was compensated");
            try { orderRepository.save(order); } catch (RuntimeException ignored) { complete = false; }
        }
        attempt.releaseClaim();
        if (complete) attempt.advanceTo(CheckoutStep.COMPENSATED);
        else attempt.fail("COMPENSATION_INCOMPLETE", "One or more checkout compensation operations failed",
                HttpStatus.SERVICE_UNAVAILABLE.value());
        attemptRepository.save(attempt);
        return complete;
    }

    private CheckoutAttempt mark(CheckoutAttempt attempt, CheckoutStep step) {
        attempt.advanceTo(step);
        return attemptRepository.save(attempt);
    }
    private String newOrderNumber() {
        String stamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now(clock));
        return "ORD-" + stamp + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
