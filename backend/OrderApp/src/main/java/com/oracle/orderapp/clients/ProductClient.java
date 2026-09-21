package com.oracle.orderapp.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.oracle.orderapp.dtos.ProductQuantityRequest;
import com.oracle.orderapp.dtos.ProductResponse;

@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(
            @Value("${product.service.url}") String productServiceUrl) {

        this.restClient = RestClient.create(productServiceUrl);
    }

    public ProductResponse getProduct(Integer productId) {
        return restClient.get()
                .uri("/{productId}", productId)
                .retrieve()
                .body(ProductResponse.class);
    }

    public void reduceQuantity(Integer productId, Integer quantity) {
        restClient.post()
                .uri("/{productId}/reduce-quantity", productId)
                .body(new ProductQuantityRequest(quantity))
                .retrieve()
                .toBodilessEntity();
    }

    public void increaseQuantity(Integer productId, Integer quantity) {
        restClient.post()
                .uri("/{productId}/increase-quantity", productId)
                .body(new ProductQuantityRequest(quantity))
                .retrieve()
                .toBodilessEntity();
    }
}
