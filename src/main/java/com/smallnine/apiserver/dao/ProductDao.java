package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductDao {
    
    /**
     * 根據ID查詢商品
     */
    Optional<Product> findById(@Param("id") Long id);
    
    /**
     * 根據SKU查詢商品
     */
    Optional<Product> findBySku(@Param("sku") String sku);
    
    /**
     * 根據分類ID查詢商品
     */
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * 根據分類ID查詢商品（分頁）
     */
    List<Product> findByCategoryIdWithPaging(@Param("categoryId") Long categoryId, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 根據品牌ID查詢商品
     */
    List<Product> findByBrandId(@Param("brandId") Long brandId);
    
    /**
     * 查詢啟用的商品（分頁）
     */
    List<Product> findActiveProducts(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 查詢所有商品（分頁）
     */
    List<Product> findAll(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 根據名稱模糊搜索商品
     */
    List<Product> searchByName(@Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 根據價格範圍查詢商品
     */
    List<Product> findByPriceRange(@Param("minPrice") java.math.BigDecimal minPrice, 
                                   @Param("maxPrice") java.math.BigDecimal maxPrice,
                                   @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 搜索商品（組合條件）
     */
    List<Product> searchProducts(@Param("keyword") String keyword,
                                @Param("minPrice") java.math.BigDecimal minPrice,
                                @Param("maxPrice") java.math.BigDecimal maxPrice,
                                @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 創建商品
     */
    int insert(Product product);
    
    /**
     * 更新商品訊息
     */
    int update(Product product);
    
    /**
     * 根據ID刪除商品
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 更新商品庫存
     */
    int updateStock(@Param("id") Long id, @Param("stockQuantity") Integer stockQuantity);
    
    /**
     * 減少商品庫存
     */
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
    
    /**
     * 統計商品總數
     */
    long count();
    
    /**
     * 統計分類下的商品數量
     */
    long countByCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * 檢查SKU是否存在
     */
    boolean existsBySku(@Param("sku") String sku);
}