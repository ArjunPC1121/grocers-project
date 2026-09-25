package com.oracle.assistantapp.dto;

public record ProductCatalogItem(Integer id, String name, String brand, String category, String subCategory,
                                 String description, String tags, String searchAliases, Double unitValue,
                                 String unitType, String imageUrl, Double price, Integer discount,
                                 Integer quantity, Boolean active) { }
