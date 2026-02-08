package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductFavoriteDao {

    List<Favorite> findByMemberId(@Param("memberId") Long memberId);

    int insert(Favorite favorite);

    int deleteByMemberIdAndProductId(@Param("memberId") Long memberId, @Param("productId") Long productId);

    boolean existsByMemberIdAndProductId(@Param("memberId") Long memberId, @Param("productId") Long productId);
}
