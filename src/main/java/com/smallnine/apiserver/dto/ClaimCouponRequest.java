package com.smallnine.apiserver.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClaimCouponRequest {
    @NotNull(message = "優惠券ID不能為空")
    private Long couponId;
}
