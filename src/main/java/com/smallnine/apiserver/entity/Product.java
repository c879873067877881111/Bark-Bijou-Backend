package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String sku;
    private Integer stockQuantity;
    private Long brandId;
    private Long categoryId;
    private Boolean isActive;
    private BigDecimal weight;
    private String dimensions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}