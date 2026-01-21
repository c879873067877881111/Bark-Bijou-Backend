package com.smallnine.apiserver.dto;

import com.smallnine.apiserver.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品回應")
public class ProductResponse {
    
    @Schema(description = "商品ID", example = "1")
    private Long id;
    
    @Schema(description = "商品名稱", example = "高級狗糧 2kg")
    private String name;
    
    @Schema(description = "商品描述", example = "營養均衡的高品質狗糧，適合成犬")
    private String description;
    
    @Schema(description = "商品價格", example = "899.00")
    private BigDecimal price;
    
    @Schema(description = "促銷價格", example = "799.00")
    private BigDecimal salePrice;
    
    @Schema(description = "商品SKU", example = "DOG-FOOD-001")
    private String sku;
    
    @Schema(description = "庫存數量", example = "50")
    private Integer stockQuantity;
    
    @Schema(description = "品牌ID", example = "3")
    private Long brandId;
    
    @Schema(description = "品牌名稱", example = "NutriPet")
    private String brandName;
    
    @Schema(description = "分類ID", example = "1")
    private Long categoryId;
    
    @Schema(description = "分類名稱", example = "食品")
    private String categoryName;
    
    @Schema(description = "是否啟用", example = "true")
    private Boolean isActive;
    
    @Schema(description = "商品重量(kg)", example = "2.0")
    private BigDecimal weight;
    
    @Schema(description = "商品尺寸", example = "30x20x10cm")
    private String dimensions;
    
    @Schema(description = "創建時間")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新時間")
    private LocalDateTime updatedAt;
    
    @Schema(description = "商品圖片列表")
    private List<ProductImageResponse> images;
    
    @Schema(description = "是否有庫存", example = "true")
    private Boolean inStock;
    
    @Schema(description = "實際售價(促銷價或原價)", example = "799.00")
    private BigDecimal actualPrice;
    
    public ProductResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.salePrice = product.getSalePrice();
        this.sku = product.getSku();
        this.stockQuantity = product.getStockQuantity();
        this.brandId = product.getBrandId();
        this.categoryId = product.getCategoryId();
        this.isActive = product.getIsActive();
        this.weight = product.getWeight();
        this.dimensions = product.getDimensions();
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();
        this.inStock = product.getStockQuantity() != null && product.getStockQuantity() > 0;
        this.actualPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
    }
}