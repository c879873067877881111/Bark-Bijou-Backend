package com.smallnine.apiserver.dto;

import com.smallnine.apiserver.entity.CartItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "購物車商品回應")
public class CartItemResponse {
    
    @Schema(description = "購物車項目ID", example = "1")
    private Long id;
    
    @Schema(description = "會員ID", example = "1")
    private Long memberId;
    
    @Schema(description = "商品ID", example = "5")
    private Long productId;
    
    @Schema(description = "商品名稱", example = "高級狗糧 2kg")
    private String productName;
    
    @Schema(description = "商品圖片", example = "/images/products/1/main.jpg")
    private String productImage;
    
    @Schema(description = "購買數量", example = "2")
    private Integer quantity;
    
    @Schema(description = "單價", example = "899.00")
    private BigDecimal unitPrice;
    
    @Schema(description = "小計", example = "1798.00")
    private BigDecimal subtotal;
    
    @Schema(description = "庫存數量", example = "50")
    private Integer stockQuantity;
    
    @Schema(description = "是否有庫存", example = "true")
    private Boolean inStock;
    
    @Schema(description = "創建時間")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新時間")
    private LocalDateTime updatedAt;
    
    public CartItemResponse(CartItem cartItem) {
        this.id = cartItem.getId();
        this.memberId = cartItem.getMemberId();
        this.productId = cartItem.getProductId();
        this.quantity = cartItem.getQuantity();
        this.unitPrice = cartItem.getUnitPrice();
        this.subtotal = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        this.createdAt = cartItem.getCreatedAt();
        this.updatedAt = cartItem.getUpdatedAt();
    }
}