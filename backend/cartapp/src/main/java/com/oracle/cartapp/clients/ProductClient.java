package com.oracle.cartapp.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.oracle.cartapp.dtos.ProductAvailabilityResponse;

@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(
          
            @Value("${product.service.url}") String productUrl) {

       this.restClient = RestClient.create(productUrl);
    }

    public void checkQuantity(Integer productId, Integer requiredQuantity) {
        ProductAvailabilityResponse product = restClient.get()
                .uri("/{id}", productId)
                .retrieve()
                .body(ProductAvailabilityResponse.class);

        if (product == null || product.quantity() < requiredQuantity) {
            throw new IllegalArgumentException("Insufficient product quantity");
        }
    }
}