package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ArticleDao {
    
    /**
     * 根據ID查詢文章
     */
    Optional<Article> findById(@Param("id") Long id);
    
    /**
     * 查詢所有文章（分頁）
     */
    List<Article> findAll(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 根據作者查詢文章
     */
    List<Article> findByAuthor(@Param("author") String author);
    
    /**
     * 根據分類查詢文章
     */
    List<Article> findByCategoryName(@Param("categoryName") String categoryName);
    
    /**
     * 搜索文章標題
     */
    List<Article> searchByTitle(@Param("title") String title, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 根據成員ID查詢文章
     */
    List<Article> findByMemberId(@Param("memberId") Long memberId);
    
    /**
     * 創建文章
     */
    int insert(Article article);
    
    /**
     * 更新文章
     */
    int update(Article article);
    
    /**
     * 刪除文章
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 統計文章總數
     */
    long count();
    
    /**
     * 統計有效文章數量
     */
    long countValid();
}