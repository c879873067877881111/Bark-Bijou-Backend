package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.dto.SitterReviewResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SitterReviewDao {

    List<SitterReviewResponse> findRecentWithMember(@Param("limit") int limit);

    List<SitterReviewResponse> findBySitterIdWithMember(@Param("sitterId") Long sitterId);

    Double getAverageRating(@Param("sitterId") Long sitterId);

    int countBySitterId(@Param("sitterId") Long sitterId);

    boolean existsByMemberAndSitter(@Param("memberId") Long memberId,
                                    @Param("sitterId") Long sitterId);

    int insert(@Param("memberId") Long memberId,
               @Param("sitterId") Long sitterId,
               @Param("rating") Integer rating,
               @Param("comment") String comment);
}
