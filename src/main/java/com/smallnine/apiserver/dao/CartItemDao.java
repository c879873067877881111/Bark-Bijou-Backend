package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CartItemDao {
    
    /**
     * 根據ID查詢購物車項目
     */
    Optional<CartItem> findById(@Param("id") Long id);
    
    /**
     * 根據用戶ID查詢購物車項目
     */
    List<CartItem> findByMemberId(@Param("memberId") Long memberId);
    
    /**
     * 根據用戶ID和商品ID查詢購物車項目
     */
    Optional<CartItem> findByMemberIdAndProductId(@Param("memberId") Long memberId, 
                                                  @Param("productId") Long productId);
    
    /**
     * 創建購物車項目
     */
    int insert(CartItem cartItem);
    
    /**
     * 更新購物車項目
     */
    int update(CartItem cartItem);
    
    /**
     * 根據ID刪除購物車項目
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根據用戶ID刪除所有購物車項目
     */
    int deleteByMemberId(@Param("memberId") Long memberId);
    
    /**
     * 根據用戶ID和商品ID刪除購物車項目
     */
    int deleteByMemberIdAndProductId(@Param("memberId") Long memberId, 
                                     @Param("productId") Long productId);
    
    /**
     * 統計用戶購物車項目數量
     */
    long countByMemberId(@Param("memberId") Long memberId);
}