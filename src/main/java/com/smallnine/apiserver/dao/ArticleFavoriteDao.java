package com.smallnine.apiserver.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleFavoriteDao {

    List<Map<String, Object>> findTopFavoritedArticleIds(@Param("limit") int limit);

    int insert(@Param("memberId") Long memberId, @Param("articleId") Long articleId);

    int deleteByMemberIdAndArticleId(@Param("memberId") Long memberId, @Param("articleId") Long articleId);

    boolean existsByMemberIdAndArticleId(@Param("memberId") Long memberId, @Param("articleId") Long articleId);

    List<Long> findArticleIdsByMemberId(@Param("memberId") Long memberId);

    int countByArticleId(@Param("articleId") Long articleId);
}
