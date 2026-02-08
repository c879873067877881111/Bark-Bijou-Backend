package com.smallnine.apiserver.dto;

import com.smallnine.apiserver.entity.Coupon;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumAmount;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private Boolean isClaimed;

    public CouponResponse(Coupon coupon, Boolean isClaimed) {
        this.id = coupon.getId();
        this.code = coupon.getCode();
        this.name = coupon.getName();
        this.description = coupon.getDescription();
        this.discountType = coupon.getDiscountType();
        this.discountValue = coupon.getDiscountValue();
        this.minimumAmount = coupon.getMinimumAmount();
        this.startsAt = coupon.getStartsAt();
        this.expiresAt = coupon.getExpiresAt();
        this.isClaimed = isClaimed;
    }
}
