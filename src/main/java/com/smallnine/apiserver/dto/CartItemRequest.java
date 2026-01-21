package com.smallnine.apiserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "購物車操作請求")
public class CartItemRequest {
    
    @NotNull(message = "商品ID不能為空")
    @Schema(description = "商品ID", example = "1", required = true)
    private Long productId;
    
    @NotNull(message = "數量不能為空")
    @Min(value = 1, message = "數量必須大於0")
    @Schema(description = "購買數量", example = "2", required = true)
    private Integer quantity;
}