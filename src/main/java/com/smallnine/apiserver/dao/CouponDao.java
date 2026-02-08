package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CouponDao {

    List<Coupon> findAllActive();

    Optional<Coupon> findById(@Param("id") Long id);
}
