package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Points;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PointsDao {

    List<Points> findByMemberId(@Param("memberId") Long memberId);

    int sumByMemberId(@Param("memberId") Long memberId);
}
