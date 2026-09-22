package com.oracle.productsapp.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oracle.productsapp.converters.FloatEmbeddingConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String category;

    @Column(name = "sub_category", length = 100)
    private String subCategory;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String tags;

    @Column(name = "search_aliases", length = 1000)
    private String searchAliases;

    @Column(name = "unit_value")
    private Double unitValue;

    @Column(name = "unit_type", length = 20)
    private String unitType;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;
    @JsonIgnore
    @Column(name = "image_public_id", length = 500)
    private String imagePublicId;

    @Column(nullable = false)
    private Double price;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    private Integer discount = 0;

    @Min(0)
    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Boolean active = true;

    // Kept below Oracle's 4,000-byte VARCHAR2 limit so it can remain in the
    // SYSTEM tablespace without creating a LOB segment.
    @JsonIgnore
    @Column(name = "search_text", length = 4000)
    private String searchText;

    // 384 FLOAT32 values consume 1,536 bytes. RAW avoids both VECTOR and LOB
    // storage restrictions in the SYSTEM tablespace.
    @JsonIgnore
    @Convert(converter = FloatEmbeddingConverter.class)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "text_embedding", length = 1536)
    private float[] textEmbedding;

    @JsonIgnore
    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @JsonIgnore
    @Column(name = "embedding_updated_at")
    private LocalDateTime embeddingUpdatedAt;
}
