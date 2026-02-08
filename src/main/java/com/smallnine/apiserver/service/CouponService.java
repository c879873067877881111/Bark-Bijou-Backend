package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dto.CouponResponse;
import com.smallnine.apiserver.entity.Coupon;
import com.smallnine.apiserver.entity.MemberCoupon;

import java.util.List;

public interface CouponService {

    List<Coupon> getAllActive();

    CouponResponse getById(Long id, Long memberId);

    MemberCoupon claim(Long couponId, Long memberId);

    List<MemberCoupon> getMemberCoupons(Long memberId);
}
