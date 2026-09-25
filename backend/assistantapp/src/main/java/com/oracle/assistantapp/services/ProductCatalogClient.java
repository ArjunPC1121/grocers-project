package com.oracle.assistantapp.services;

import com.oracle.assistantapp.dto.ProductCatalogItem;

import java.util.List;

public interface ProductCatalogClient {
    List<ProductCatalogItem> getProducts();
}
