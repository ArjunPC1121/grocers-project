package com.oracle.orderapp.clients;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.oracle.orderapp.dtos.PaymentRequest;
import com.oracle.orderapp.dtos.OrderCustomerDetails;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(
            @Value("${user.service.url}") String userServiceUrl) {

        this.restClient = RestClient.create(userServiceUrl);
    }

    public void debit(Integer userId, BigDecimal amount, String orderNumber) {
        restClient.post()
                .uri("/{userId}/debit", userId)
                .body(new PaymentRequest(amount, orderNumber))
                .retrieve()
                .toBodilessEntity();
    }

    public void refund(Integer userId, BigDecimal amount, String orderNumber) {
        restClient.post()
                .uri("/{userId}/refund", userId)
                .body(new PaymentRequest(amount, orderNumber))
                .retrieve()
                .toBodilessEntity();
    }
    public void checkUserExists(Integer userId) {
        restClient.get()
                .uri("/{userId}", userId)
                .retrieve()
                .toBodilessEntity();
    }

    public OrderCustomerDetails getCustomer(Integer userId) {
        return restClient.get()
                .uri("/{userId}", userId)
                .retrieve()
                .body(OrderCustomerDetails.class);
    }
}
