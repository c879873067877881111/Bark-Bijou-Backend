package com.smallnine.apiserver.dto;

import com.smallnine.apiserver.entity.ProductImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品圖片回應")
public class ProductImageResponse {
    
    @Schema(description = "圖片ID", example = "1")
    private Long id;
    
    @Schema(description = "商品ID", example = "5")
    private Long productId;
    
    @Schema(description = "圖片URL", example = "https://example.com/images/product1.jpg")
    private String imageUrl;
    
    @Schema(description = "是否為主圖", example = "true")
    private Boolean isPrimary;
    
    @Schema(description = "排序順序", example = "1")
    private Integer sortOrder;
    
    @Schema(description = "創建時間")
    private LocalDateTime createdAt;
    
    public ProductImageResponse(ProductImage productImage) {
        this.id = productImage.getId();
        this.productId = productImage.getProductId();
        this.imageUrl = productImage.getImageUrl();
        this.isPrimary = productImage.getIsPrimary();
        this.sortOrder = productImage.getSortOrder();
        this.createdAt = productImage.getCreatedAt();
    }
}