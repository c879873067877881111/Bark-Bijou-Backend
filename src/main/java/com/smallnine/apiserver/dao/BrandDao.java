package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Brand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface BrandDao {
    
    /**
     * 根據ID查詢品牌
     */
    Optional<Brand> findById(@Param("id") Long id);
    
    /**
     * 根據名稱查詢品牌
     */
    Optional<Brand> findByName(@Param("name") String name);
    
    /**
     * 查詢所有品牌（分頁）
     */
    List<Brand> findAll(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 根據名稱模糊搜索品牌
     */
    List<Brand> searchByName(@Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 創建品牌
     */
    int insert(Brand brand);
    
    /**
     * 更新品牌訊息
     */
    int update(Brand brand);
    
    /**
     * 根據ID刪除品牌
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 統計品牌總數
     */
    long count();
    
    /**
     * 檢查品牌名稱是否存在
     */
    boolean existsByName(@Param("name") String name);
    
    /**
     * 統計品牌下的商品數量
     */
    long countProductsByBrandId(@Param("brandId") Long brandId);
}