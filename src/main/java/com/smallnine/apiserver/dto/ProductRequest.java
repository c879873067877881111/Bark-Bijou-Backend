package com.smallnine.apiserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品創建/更新請求")
public class ProductRequest {
    
    @NotBlank(message = "商品名稱不能為空")
    @Size(max = 255, message = "商品名稱長度不能超過255字符")
    @Schema(description = "商品名稱", example = "高級狗糧 2kg", required = true)
    private String name;
    
    @Size(max = 2000, message = "商品描述長度不能超過2000字符")
    @Schema(description = "商品描述", example = "營養均衡的高品質狗糧，適合成犬")
    private String description;
    
    @NotNull(message = "商品價格不能為空")
    @DecimalMin(value = "0.01", message = "商品價格必須大於0")
    @Digits(integer = 8, fraction = 2, message = "價格格式不正確")
    @Schema(description = "商品價格", example = "899.00", required = true)
    private BigDecimal price;
    
    @DecimalMin(value = "0.01", message = "促銷價格必須大於0")
    @Digits(integer = 8, fraction = 2, message = "促銷價格格式不正確")
    @Schema(description = "促銷價格", example = "799.00")
    private BigDecimal salePrice;
    
    @Size(max = 100, message = "SKU長度不能超過100字符")
    @Schema(description = "商品SKU", example = "DOG-FOOD-001")
    private String sku;
    
    @Min(value = 0, message = "庫存數量不能為負數")
    @Schema(description = "庫存數量", example = "50")
    private Integer stockQuantity = 0;
    
    @Schema(description = "品牌ID", example = "3")
    private Long brandId;
    
    @Schema(description = "分類ID", example = "1")
    private Long categoryId;
    
    @Schema(description = "是否啟用", example = "true")
    private Boolean isActive = true;
    
    @DecimalMin(value = "0.01", message = "重量必須大於0")
    @Digits(integer = 6, fraction = 2, message = "重量格式不正確")
    @Schema(description = "商品重量(kg)", example = "2.0")
    private BigDecimal weight;
    
    @Size(max = 100, message = "尺寸訊息長度不能超過100字符")
    @Schema(description = "商品尺寸", example = "30x20x10cm")
    private String dimensions;
}