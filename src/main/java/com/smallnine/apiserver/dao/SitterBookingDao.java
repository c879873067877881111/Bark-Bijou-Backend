package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.SitterBooking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface SitterBookingDao {

    boolean existsByMemberAndSitter(@Param("memberId") Long memberId,
                                    @Param("sitterId") Long sitterId);

    int insert(SitterBooking booking);

    Optional<SitterBooking> findById(@Param("id") Long id);
}
