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
@Schema(description = "品牌創建/更新請求")
public class BrandRequest {
    
    @NotBlank(message = "品牌名稱不能為空")
    @Size(max = 100, message = "品牌名稱長度不能超過100字符")
    @Schema(description = "品牌名稱", example = "Royal Canin", required = true)
    private String name;
    
    @Size(max = 1000, message = "品牌描述長度不能超過1000字符")
    @Schema(description = "品牌描述", example = "專業寵物營養品牌")
    private String description;
    
    @Size(max = 255, message = "品牌Logo URL長度不能超過255字符")
    @Schema(description = "品牌Logo URL", example = "https://example.com/brand-logo.jpg")
    private String logoUrl;
}