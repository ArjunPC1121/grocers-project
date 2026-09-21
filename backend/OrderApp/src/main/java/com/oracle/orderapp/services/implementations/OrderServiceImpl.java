package com.oracle.orderapp.services.implementations;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.oracle.orderapp.clients.CartClient;
import com.oracle.orderapp.dtos.*;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final CartClient cartClient;
    private boolean isValidEmployeeStatusChange(
            OrderStatus currentStatus,
            OrderStatus newStatus) {

        return (currentStatus == OrderStatus.PLACED
                && newStatus == OrderStatus.SHIPPED)

                || (currentStatus == OrderStatus.SHIPPED
                && newStatus == OrderStatus.OUT_FOR_DELIVERY)

                || (currentStatus == OrderStatus.OUT_FOR_DELIVERY
                && newStatus == OrderStatus.DELIVERED)

                || (currentStatus == OrderStatus.PLACED
                && newStatus == OrderStatus.CANCELLED);
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
            releaseReducedStock(reducedItems);

            order.setStatus(OrderStatus.STOCK_REJECTED);
            return orderRepository.save(order);
        }

        try {
            userClient.debit(
                    order.getCustomerId(),
                    order.getTotalAmount(),
                    order.getOrderNumber()
            );
        } catch (Exception exception) {
            releaseReducedStock(order.getItems());

            order.setStatus(OrderStatus.PAYMENT_FAILED);
            return orderRepository.save(order);
        }

        order.setStatus(OrderStatus.PLACED);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancel(Integer orderId, String cancellationReason) {
        Order order = getById(orderId);

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    "Only PLACED orders can be cancelled"
            );
        }

        return cancelOrder(order, cancellationReason, null);
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
    @Override
    @Transactional
    public Order updateStatus(
            Integer orderId,
            UpdateOrderStatusRequest request,
            Integer employeeId) {

        Order order = getById(orderId);

        if (!isValidEmployeeStatusChange(
                order.getStatus(),
                request.status())) {

            throw new IllegalStateException(
                    "Invalid status change: "
                            + order.getStatus() + " to " + request.status()
            );
        }

        if (request.status() == OrderStatus.CANCELLED) {
            if (request.cancellationReason() == null || request.cancellationReason().isBlank()) {
                throw new IllegalArgumentException("A cancellation reason is required");
            }
            return cancelOrder(order, request.cancellationReason(), employeeId);
        }

        order.setStatus(request.status());
        order.setUpdatedByEmployeeId(employeeId);

        return orderRepository.save(order);
    }
    @Override
    public List<Order> getByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    private Order cancelOrder(Order order, String cancellationReason, Integer employeeId) {
        userClient.refund(
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getOrderNumber()
        );

        for (OrderItem item : order.getItems()) {
            productClient.increaseQuantity(item.getProductId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(cancellationReason);
        order.setUpdatedByEmployeeId(employeeId);
        return orderRepository.save(order);
    }
}
