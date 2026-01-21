package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    
    private Long id;
    private String code;
    private String name;
    private String description;
    private String discountType; // 'percentage' or 'fixed'
    private BigDecimal discountValue;
    private BigDecimal minimumAmount;
    private Integer maximumUses;
    private Integer usedCount;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private LocalDateTime createdAt;
    
    public enum DiscountType {
        PERCENTAGE,  // 百分比折扣
        FIXED        // 固定金額
    }
}