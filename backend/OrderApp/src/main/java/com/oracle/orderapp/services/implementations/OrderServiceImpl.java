package com.oracle.orderapp.services.implementations;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.oracle.orderapp.clients.CartClient;
import com.oracle.orderapp.clients.EmployeeClient;
import com.oracle.orderapp.dtos.*;
import com.oracle.orderapp.entities.PaymentMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oracle.orderapp.clients.ProductClient;
import com.oracle.orderapp.clients.UserClient;
import com.oracle.orderapp.entities.Order;
import com.oracle.orderapp.entities.OrderItem;
import com.oracle.orderapp.entities.OrderStatus;
import com.oracle.orderapp.exceptions.ResourceNotFoundException;
import com.oracle.orderapp.repository.OrderRepository;
import com.oracle.orderapp.services.abstractions.OrderService;
import com.oracle.orderapp.events.OrderCheckoutEventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final CartClient cartClient;
    private final EmployeeClient employeeClient;
    private final OrderCheckoutEventPublisher orderCheckoutEventPublisher;

    private boolean isValidEmployeeStatusChange(
            OrderStatus currentStatus,
            OrderStatus newStatus) {

        return (currentStatus == OrderStatus.PLACED
                && newStatus == OrderStatus.SHIPPED)

                || (currentStatus == OrderStatus.SHIPPED
                && newStatus == OrderStatus.OUT_FOR_DELIVERY)

                || (currentStatus == OrderStatus.OUT_FOR_DELIVERY
                && newStatus == OrderStatus.DELIVERED);
    }

    @Override
    @Transactional
    public Order create(CreateOrderRequest request) {
        userClient.checkUserExists(request.customerId());
        CartResponse cart = cartClient.getCart(request.cartId());

        if (!cart.userId().equals(request.customerId())) {
            throw new IllegalArgumentException(
                    "This cart does not belong to customer: " + request.customerId()
            );
        }

        if (!"ACTIVE".equals(cart.status())) {
            throw new IllegalStateException("Cart is not active");
        }
        Order order = new Order();

        order.setOrderNumber(generateOrderNumber());
        order.setCustomerId(request.customerId());
        order.setCartId(request.cartId());
        order.setDeliveryAddress(request.deliveryAddress());
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentMethod(request.paymentMethod());
        
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest requestItem : request.items()) {
            ProductResponse product =
                    productClient.getProduct(requestItem.productId());

            if (product == null) {
                throw new ResourceNotFoundException(
                        "Product not found: " + requestItem.productId()
                );
            }

            BigDecimal unitPrice = getDiscountedPrice(product);
            BigDecimal subtotal = unitPrice.multiply(
                    BigDecimal.valueOf(requestItem.quantity())
            );

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(product.id());
            item.setProductName(product.name());
            item.setQuantity(requestItem.quantity());
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);

            order.getItems().add(item);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        return orderRepository.save(order);
    }

    @Override
    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeOrderDetails> getAllEmployeeDetails(Integer employeeId) {
        return orderRepository.findAll().stream()
                .filter(order -> order.getUpdatedByEmployeeId() == null
                        || employeeId.equals(order.getUpdatedByEmployeeId()))
                .map(this::employeeDetails).toList();
    }

    @Override
    public Order getById(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found: " + orderId)
                );
    }

    @Override
    public List<Order> getByCustomerId(Integer customerId) {
        return orderRepository.findByCustomerId(customerId);
    }



    
    @Override
    @Transactional
    public Order checkout(Integer orderId) {
        Order order = getById(orderId);

        if (order.getPaymentMethod() == null) {
            throw new IllegalStateException("Order has no payment method");
        }

        if (order.getPaymentMethod() == PaymentMethod.CARD) {
            throw new IllegalStateException(
                    "Card payments are not available yet. Choose Funds or Cash on Delivery."
            );
        }

        if (order.getStatus() != OrderStatus.CREATED
                && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw new IllegalStateException(
                    "Only CREATED or PAYMENT_FAILED orders can be checked out"
            );
        }

        List<OrderItem> reducedItems = new ArrayList<>();

        try {
            for (OrderItem item : order.getItems()) {
                productClient.reduceQuantity(
                        item.getProductId(),
                        item.getQuantity()
                );

                reducedItems.add(item);
            }
        } catch (Exception exception) {
            // Temporary: check Order-app console for the real Product-service error.
            exception.printStackTrace();

            releaseReducedStock(reducedItems);

            order.setStatus(OrderStatus.STOCK_REJECTED);
            return orderRepository.save(order);
        }

        if (order.getPaymentMethod() == PaymentMethod.FUNDS) {
            try {
                userClient.debit(
                        order.getCustomerId(),
                        order.getTotalAmount(),
                        order.getOrderNumber()
                );
            } catch (Exception exception) {
                exception.printStackTrace();

                releaseReducedStock(order.getItems());

                order.setStatus(OrderStatus.PAYMENT_FAILED);
                return orderRepository.save(order);
            }
        }

        // Requires cartClient.checkoutCart(Integer cartId).
        // Cart status should change from ACTIVE to CHECKED_OUT.
        cartClient.checkoutCart(order.getCartId());

        order.setStatus(OrderStatus.PLACED);
        Order checkedOutOrder = orderRepository.save(order);

        orderCheckoutEventPublisher.publish(
                new OrderCheckedOutEvent(
                        checkedOutOrder.getId(),
                        checkedOutOrder.getCustomerId(),
                        checkedOutOrder.getOrderNumber(),
                        checkedOutOrder.getTotalAmount(),
                        checkedOutOrder.getStatus().name(),
                        checkedOutOrder.getUpdatedAt()
                )
        );

        return checkedOutOrder;
    }

    @Override
    @Transactional
    public Order cancel(Integer orderId, String cancellationReason) {
        Order order = getById(orderId);

        return cancelOrder(order, cancellationReason, null);
    }

    @Override
    @Transactional
    public Order cancelByEmployee(Integer orderId, String cancellationReason, Integer employeeId) {
        employeeClient.checkEmployeeExists(employeeId);
        Order order = getById(orderId);
        claimableBy(order, employeeId);
        return cancelOrder(order, cancellationReason, employeeId);
    }

    private Order cancelOrder(Order order, String cancellationReason, Integer employeeId) {

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    "Only PLACED orders can be cancelled"
            );
        }

        if (order.getPaymentMethod() == PaymentMethod.FUNDS) {
            userClient.refund(
                    order.getCustomerId(),
                    order.getTotalAmount(),
                    order.getOrderNumber()
            );
        }

        for (OrderItem item : order.getItems()) {
            productClient.increaseQuantity(
                    item.getProductId(),
                    item.getQuantity()
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        if (employeeId != null) order.setUpdatedByEmployeeId(employeeId);
        order.setCancellationReason(cancellationReason);

        Order cancelledOrder = orderRepository.save(order);

        orderCheckoutEventPublisher.publish(
                new OrderCheckedOutEvent(
                        cancelledOrder.getId(),
                        cancelledOrder.getCustomerId(),
                        cancelledOrder.getOrderNumber(),
                        cancelledOrder.getTotalAmount(),
                        cancelledOrder.getStatus().name(),
                        cancelledOrder.getUpdatedAt()
                )
        );

        return cancelledOrder;
    }

    @Override
    @Transactional
    public void deleteCreatedOrder(Integer orderId) {
        Order order = getById(orderId);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Only CREATED orders can be deleted"
            );
        }

        orderRepository.delete(order);
    }

    @Override
    @Transactional
    public Order updateAddress(
            Integer orderId,
            UpdateOrderAddressRequest request) {

        Order order = getById(orderId);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Address can be updated only before checkout"
            );
        }

        order.setDeliveryAddress(request.deliveryAddress());
        return orderRepository.save(order);
    }

    private void claimableBy(Order order, Integer employeeId) {
        if (order.getUpdatedByEmployeeId() != null && !employeeId.equals(order.getUpdatedByEmployeeId())) {
            throw new IllegalStateException("This order is already being handled by another employee");
        }
    }

    private void releaseReducedStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            productClient.increaseQuantity(
                    item.getProductId(),
                    item.getQuantity()
            );
        }
    }

    private BigDecimal getDiscountedPrice(ProductResponse product) {
        BigDecimal originalPrice = BigDecimal.valueOf(product.price());

        int discount = product.discount() == null
                ? 0
                : product.discount();

        return originalPrice
                .multiply(BigDecimal.valueOf(100 - discount))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase(Locale.ROOT);
    }

    private EmployeeOrderDetails employeeDetails(Order order) {
        List<OrderItemDetails> items = order.getItems().stream()
                .map(item -> new OrderItemDetails(item.getProductId(), item.getProductName(),
                        item.getQuantity(), item.getUnitPrice(), item.getSubtotal()))
                .toList();
        return new EmployeeOrderDetails(order.getId(), order.getOrderNumber(), order.getCustomerId(),
                userClient.getCustomer(order.getCustomerId()), order.getStatus().name(), order.getTotalAmount(),
                order.getDeliveryAddress(), items, order.getCancellationReason(), order.getOrderedAt(), order.getUpdatedAt(),
                order.getUpdatedByEmployeeId());
    }
    @Override
    @Transactional
    public Order updateStatus(
            Integer orderId,
            UpdateOrderStatusRequest request) {

        employeeClient.checkEmployeeExists(request.employeeId());

        Order order = getById(orderId);

        claimableBy(order, request.employeeId());

        if (!isValidEmployeeStatusChange(
                order.getStatus(),
                request.status())) {

            throw new IllegalStateException(
                    "Invalid status change: "
                            + order.getStatus() + " to " + request.status()
            );
        }

        order.setStatus(request.status());
        order.setUpdatedByEmployeeId(request.employeeId());

        Order updatedOrder = orderRepository.save(order);

        // Keep UserApp's Kafka-backed customer order summary in sync with fulfilment updates.
        orderCheckoutEventPublisher.publish(
                new OrderCheckedOutEvent(
                        updatedOrder.getId(),
                        updatedOrder.getCustomerId(),
                        updatedOrder.getOrderNumber(),
                        updatedOrder.getTotalAmount(),
                        updatedOrder.getStatus().name(),
                        updatedOrder.getUpdatedAt()
                )
        );

        return updatedOrder;
    }
    @Override
    public List<Order> getByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
}
