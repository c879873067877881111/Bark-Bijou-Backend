package com.smallnine.apiserver.dto;

import com.smallnine.apiserver.entity.Brand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Schema(description = "品牌回應")
public class BrandResponse {
    
    @Schema(description = "品牌ID", example = "1")
    private Long id;
    
    @Schema(description = "品牌名稱", example = "Royal Canin")
    private String name;
    
    @Schema(description = "品牌描述", example = "專業寵物營養品牌")
    private String description;
    
    @Schema(description = "品牌Logo URL", example = "https://example.com/brand-logo.jpg")
    private String logoUrl;
    
    @Schema(description = "創建時間")
    private LocalDateTime createdAt;
    
    @Schema(description = "商品數量")
    private Long productCount;
    
    public BrandResponse(Brand brand) {
        this.id = brand.getId();
        this.name = brand.getName();
        this.description = brand.getDescription();
        this.logoUrl = brand.getLogoUrl();
        this.createdAt = brand.getCreatedAt();
    }
    
    public static BrandResponse fromEntity(Brand brand) {
        return new BrandResponse(brand);
    }
}