package com.oracle.productsapp.services.implementations;

import com.oracle.productsapp.services.abstractions.ProductEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductEmbeddingServiceImpl
        implements ProductEmbeddingService {

    private final EmbeddingModel embeddingModel;

    @Value("${product.search.embedding-dimensions:384}")
    private int expectedDimensions;

    @Override
    public float[] embed(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException(
                    "Text to embed cannot be blank"
            );
        }

        float[] embedding = embeddingModel.embed(text.trim());

        if (embedding == null) {
            throw new IllegalStateException(
                    "Embedding model returned null"
            );
        }

        if (embedding.length != expectedDimensions) {
            throw new IllegalStateException(
                    "Expected embedding dimension "
                            + expectedDimensions
                            + " but model returned "
                            + embedding.length
            );
        }

        return embedding;
    }
}