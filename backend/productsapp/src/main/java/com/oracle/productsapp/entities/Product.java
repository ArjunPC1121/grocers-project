package com.oracle.productsapp.entities;




import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Integer id;
    @NotBlank(message = "Product name cannot be blank")
    @Column(nullable = false, length = 100)
    private String name;


@Column(nullable = false)
private Double price;

@Min(0)
@Max(100)
@Column(nullable = false)
private Integer discount = 0;

@Min(value = 0)
    @Column(nullable = false)
    private Integer quantity;


// @Version
// private Long version;
}
