package com.smallnine.apiserver.dto;

import com.smallnine.apiserver.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "分類回應")
public class CategoryResponse {
    
    @Schema(description = "分類ID", example = "1")
    private Long id;
    
    @Schema(description = "分類名稱", example = "寵物用品")
    private String name;
    
    @Schema(description = "分類描述", example = "各種寵物日常用品")
    private String description;
    
    @Schema(description = "父分類ID", example = "1")
    private Long parentId;
    
    @Schema(description = "分類圖片URL", example = "https://example.com/category.jpg")
    private String imageUrl;
    
    @Schema(description = "是否啟用", example = "true")
    private Boolean isActive;
    
    @Schema(description = "創建時間")
    private LocalDateTime createdAt;
    
    @Schema(description = "子分類列表")
    private List<CategoryResponse> children;
    
    @Schema(description = "商品數量")
    private Long productCount;
    
    public CategoryResponse(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.description = category.getDescription();
        this.parentId = category.getParentId();
        this.imageUrl = category.getImageUrl();
        this.isActive = category.getIsActive();
        this.createdAt = category.getCreatedAt();
    }
    
    public static CategoryResponse fromEntity(Category category) {
        return new CategoryResponse(category);
    }
}