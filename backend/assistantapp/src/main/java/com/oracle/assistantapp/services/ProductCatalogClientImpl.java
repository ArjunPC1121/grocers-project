package com.oracle.assistantapp.services;

import com.oracle.assistantapp.dto.ProductCatalogItem;
import com.oracle.assistantapp.exceptions.AssistantUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class ProductCatalogClientImpl implements ProductCatalogClient {
    private final RestClient productsClient;

    public ProductCatalogClientImpl(@Value("${services.products-url}") String productsUrl) {
        this.productsClient = RestClient.create(productsUrl);
    }

    @Override
    public List<ProductCatalogItem> getProducts() {
        try {
            List<ProductCatalogItem> products = productsClient.get().retrieve()
                    .body(new ParameterizedTypeReference<List<ProductCatalogItem>>() { });
            return products == null ? List.of() : products;
        } catch (RestClientException exception) {
            throw new AssistantUnavailableException("Products App is unavailable. Please try again shortly.", exception);
        }
    }
}
