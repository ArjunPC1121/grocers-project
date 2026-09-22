package com.oracle.assistantapp.dto;

import java.math.BigDecimal;

public record ProductCatalogItem(Integer id, String name, BigDecimal price, Integer discount, Integer quantity) { }
