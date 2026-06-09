package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductReviewDao {

    List<Review> findByProductId(@Param("productId") Long productId);

    Optional<Review> findById(@Param("id") Long id);

    boolean existsByMemberIdAndProductId(@Param("memberId") Long memberId, @Param("productId") Long productId);

    int insert(Review review);

    int update(Review review);

    int deleteById(@Param("id") Long id);
}
