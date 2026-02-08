package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberCoupon {
    private Long id;
    private Long memberId;
    private Long couponId;
    private LocalDateTime usedAt;
    private Long orderId;
    private LocalDateTime acquiredAt;
    private String source;
}
