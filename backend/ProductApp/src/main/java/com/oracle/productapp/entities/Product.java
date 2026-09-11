package com.oracle.productapp.entities;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


// int id,
// product name 
// product price
// product quantity
@Entity 
@Data
@Table(name = "product")
@AllArgsConstructor 
@NoArgsConstructor 

public class Product {
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)

private Integer id;
@NotBlank(message = "Product name cannot be blank")
@Column(nullable = false, length = 100)
private String name;



@Column(nullable = false, precision = 12, scale = 2)
private BigDecimal price;

@Min(value = 0)
@Column(nullable = false)
private Integer quantity;

// @Version
// private Long version;
}