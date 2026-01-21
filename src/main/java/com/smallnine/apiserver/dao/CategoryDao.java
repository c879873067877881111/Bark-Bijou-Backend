package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CategoryDao {
    
    /**
     * 根據ID查詢分類
     */
    Optional<Category> findById(@Param("id") Long id);
    
    /**
     * 根據名稱查詢分類
     */
    Optional<Category> findByName(@Param("name") String name);
    
    /**
     * 查詢所有分類（分頁）
     */
    List<Category> findAll(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 查詢啟用的分類（分頁）
     */
    List<Category> findActiveCategories(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 根據父分類ID查詢子分類
     */
    List<Category> findByParentId(@Param("parentId") Long parentId);
    
    /**
     * 查詢頂級分類（parentId為null）
     */
    List<Category> findTopCategories();
    
    /**
     * 創建分類
     */
    int insert(Category category);
    
    /**
     * 更新分類訊息
     */
    int update(Category category);
    
    /**
     * 根據ID刪除分類
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 更新分類狀態
     */
    int updateStatus(@Param("id") Long id, @Param("isActive") Boolean isActive);
    
    /**
     * 統計分類總數
     */
    long count();
    
    /**
     * 統計啟用的分類數量
     */
    long countActive();
    
    /**
     * 統計子分類數量
     */
    long countByParentId(@Param("parentId") Long parentId);
    
    /**
     * 檢查分類名稱是否存在
     */
    boolean existsByName(@Param("name") String name);
    
    /**
     * 檢查分類是否有子分類
     */
    boolean hasChildren(@Param("id") Long id);
}