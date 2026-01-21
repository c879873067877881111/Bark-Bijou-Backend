package com.smallnine.apiserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分類創建/更新請求")
public class CategoryRequest {
    
    @NotBlank(message = "分類名稱不能為空")
    @Size(max = 100, message = "分類名稱長度不能超過100字符")
    @Schema(description = "分類名稱", example = "寵物用品", required = true)
    private String name;
    
    @Size(max = 500, message = "分類描述長度不能超過500字符")
    @Schema(description = "分類描述", example = "各種寵物日常用品")
    private String description;
    
    @Schema(description = "父分類ID", example = "1")
    private Long parentId;
    
    @Size(max = 255, message = "圖片URL長度不能超過255字符")
    @Schema(description = "分類圖片URL", example = "https://example.com/category.jpg")
    private String imageUrl;
    
    @Schema(description = "是否啟用", example = "true")
    private Boolean isActive = true;
}