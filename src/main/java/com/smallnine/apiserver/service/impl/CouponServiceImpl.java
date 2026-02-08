package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.CouponDao;
import com.smallnine.apiserver.dao.MemberCouponDao;
import com.smallnine.apiserver.dto.CouponResponse;
import com.smallnine.apiserver.entity.Coupon;
import com.smallnine.apiserver.entity.MemberCoupon;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponDao couponDao;
    private final MemberCouponDao memberCouponDao;

    @Override
    public List<Coupon> getAllActive() {
        return couponDao.findAllActive();
    }

    @Override
    public CouponResponse getById(Long id, Long memberId) {
        Coupon coupon = couponDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.COUPON_NOT_FOUND));
        boolean isClaimed = memberId != null && memberCouponDao.existsByMemberIdAndCouponId(memberId, id);
        return new CouponResponse(coupon, isClaimed);
    }

    @Override
    @Transactional
    public MemberCoupon claim(Long couponId, Long memberId) {
        Coupon coupon = couponDao.findById(couponId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COUPON_NOT_FOUND));

        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResponseCode.COUPON_EXPIRED);
        }

        if (coupon.getMaximumUses() != null && coupon.getUsedCount() != null
                && coupon.getUsedCount() >= coupon.getMaximumUses()) {
            throw new BusinessException(ResponseCode.COUPON_EXPIRED, "優惠券已被領完");
        }

        if (memberCouponDao.existsByMemberIdAndCouponId(memberId, couponId)) {
            throw new BusinessException(ResponseCode.COUPON_ALREADY_CLAIMED);
        }

        MemberCoupon mc = new MemberCoupon();
        mc.setMemberId(memberId);
        mc.setCouponId(couponId);
        mc.setSource("claim");
        memberCouponDao.insert(mc);
        return mc;
    }

    @Override
    public List<MemberCoupon> getMemberCoupons(Long memberId) {
        return memberCouponDao.findByMemberId(memberId);
    }
}
