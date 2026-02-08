package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.MemberCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MemberCouponDao {

    List<MemberCoupon> findByMemberId(@Param("memberId") Long memberId);

    Optional<MemberCoupon> findByMemberIdAndCouponId(@Param("memberId") Long memberId, @Param("couponId") Long couponId);

    int insert(MemberCoupon memberCoupon);

    boolean existsByMemberIdAndCouponId(@Param("memberId") Long memberId, @Param("couponId") Long couponId);
}
