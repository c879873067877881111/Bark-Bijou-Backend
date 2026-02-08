package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.ArticleComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ArticleCommentDao {

    List<ArticleComment> findByArticleId(@Param("articleId") Long articleId);

    Optional<ArticleComment> findById(@Param("id") Long id);

    int insert(ArticleComment comment);

    int deleteById(@Param("id") Long id);
}
