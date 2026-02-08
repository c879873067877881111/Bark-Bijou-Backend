package com.smallnine.apiserver.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FavoriteRequest {
    @NotNull(message = "商品ID不能為空")
    private Long productId;
}
