package com.oracle.productsapp.services.abstractions;

public interface ProductEmbeddingService {

    float[] embed(String text);
}