package com.oracle.productsapp.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the user-facing category label and reads both labels and old slugs. */
@Converter
public class ProductCategoryConverter implements AttributeConverter<ProductCategory, String> {

    @Override
    public String convertToDatabaseColumn(ProductCategory category) {
        return category == null ? null : category.getDisplayName();
    }

    @Override
    public ProductCategory convertToEntityAttribute(String value) {
        return ProductCategory.fromDatabaseValue(value);
    }
}
