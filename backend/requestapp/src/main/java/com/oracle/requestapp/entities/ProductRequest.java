package com.oracle.requestapp.entities;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "product_requests")
public class ProductRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "product_id")
    private Integer productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestAction action;

    @Column(name = "product_name", length = 150)
    private String name;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String category;

    @Column(name = "sub_category", length = 100)
    private String subCategory;

    @Column(name = "requested_price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "requested_quantity")
    private Integer quantity;

    @Column(name = "requested_discount")
    private Integer discount;

    @Column(length = 2000)
    private String description;

    @Column(length = 1000)
    private String tags;

    @Column(name = "search_aliases", length = 1000)
    private String searchAliases;

    @Column(name = "unit_value")
    private Double unitValue;

    @Column(name = "unit_type", length = 20)
    private String unitType;

    private Boolean active;

    @Lob
    @Column(name = "image_data")
    private String imageData;

    @Column(name = "image_file_name", length = 255)
    private String imageFileName;

    @Column(name = "request_reason", length = 2000)
    private String reason;

    @Lob
    @Column(name = "previous_values")
    private String previousValues;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "reviewed_by_admin_id")
    private Integer reviewedByAdminId;

    protected ProductRequest() {
        
    }

    public ProductRequest(Integer employeeId, Integer productId, RequestAction action,
                          String name, String brand, String category, String subCategory,
                          BigDecimal price, Integer quantity, Integer discount, String description,
                          String tags, String searchAliases, Double unitValue, String unitType,
                          Boolean active, String imageData, String imageFileName, String reason,
                          String previousValues) {
        this.employeeId = employeeId;
        this.productId = productId;
        this.action = action;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.subCategory = subCategory;
        this.price = price;
        this.quantity = quantity;
        this.discount = discount;
        this.description = description;
        this.tags = tags;
        this.searchAliases = searchAliases;
        this.unitValue = unitValue;
        this.unitType = unitType;
        this.active = active;
        this.imageData = imageData;
        this.imageFileName = imageFileName;
        this.reason = reason;
        this.previousValues = previousValues;
        this.status = RequestStatus.PENDING;
    }

    public Integer getRequestId() { return requestId; }
    public Integer getEmployeeId() { return employeeId; }
    public Integer getProductId() { return productId; }
    public RequestAction getAction() { return action; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getCategory() { return category; }
    public String getSubCategory() { return subCategory; }
    public BigDecimal getPrice() { return price; }
    public Integer getQuantity() { return quantity; }
    public Integer getDiscount() { return discount; }
    public String getDescription() { return description; }
    public String getTags() { return tags; }
    public String getSearchAliases() { return searchAliases; }
    public Double getUnitValue() { return unitValue; }
    public String getUnitType() { return unitType; }
    public Boolean getActive() { return active; }
    public String getImageData() { return imageData; }
    public String getImageFileName() { return imageFileName; }
    public String getReason() { return reason; }
    public String getPreviousValues() { return previousValues; }
    public RequestStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public Integer getReviewedByAdminId() { return reviewedByAdminId; }

    public void review(RequestStatus status, String rejectionReason, Integer adminId) {
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.reviewedByAdminId = adminId;
    }
}
