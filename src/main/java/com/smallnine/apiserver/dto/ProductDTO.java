package com.smallnine.apiserver.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

@Data
public class ProductDTO {
    
    @NotBlank(message = "商品名稱不能為空")
    private String name;
    
    private String description;
    
    @NotNull(message = "價格不能為空")
    @DecimalMin(value = "0.01", message = "價格必須大於0")
    private BigDecimal price;
    
    @DecimalMin(value = "0.00", message = "折扣價不能為負")
    private BigDecimal salePrice;
    
    private String sku;
    
    @Min(value = 0, message = "庫存數量不能為負")
    private Integer stockQuantity = 0;
    
    private Long brandId;
    
    private Long categoryId;
    
    private Boolean isActive = true;
    
    @DecimalMin(value = "0.00", message = "重量不能為負")
    private BigDecimal weight;
    
    private String dimensions;
}