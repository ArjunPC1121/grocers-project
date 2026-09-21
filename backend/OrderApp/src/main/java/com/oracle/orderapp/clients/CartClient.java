package com.oracle.orderapp.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.oracle.orderapp.dtos.CartResponse;

@Component
public class CartClient {

    private final RestClient restClient;

    public CartClient(
            @Value("${cart.service.url}") String cartServiceUrl) {

        this.restClient = RestClient.create(cartServiceUrl);
    }

    public CartResponse getCart(Integer cartId) {
        return restClient.get()
                .uri("/{cartId}", cartId)
                .retrieve()
                .body(CartResponse.class);
    }
    public void checkoutCart(Integer cartId) {
        restClient.patch()
                .uri("/{cartId}/checkout", cartId)
                .retrieve()
                .toBodilessEntity();
    }
}
