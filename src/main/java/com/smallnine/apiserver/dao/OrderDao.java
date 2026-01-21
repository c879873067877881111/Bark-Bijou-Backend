package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface OrderDao {
    
    /**
     * 根據ID查詢訂單
     */
    Optional<Order> findById(@Param("id") Long id);
    
    /**
     * 根據訂單號查詢訂單
     */
    Optional<Order> findByOrderNumber(@Param("orderNumber") String orderNumber);
    
    /**
     * 根據用戶ID查詢訂單列表
     */
    List<Order> findByMemberId(@Param("memberId") Long memberId, 
                               @Param("offset") int offset, 
                               @Param("limit") int limit);
    
    /**
     * 根據狀態查詢訂單列表
     */
    List<Order> findByStatusId(@Param("statusId") Long statusId,
                               @Param("offset") int offset,
                               @Param("limit") int limit);
    
    /**
     * 創建訂單
     */
    int insert(Order order);
    
    /**
     * 更新訂單
     */
    int update(Order order);
    
    /**
     * 更新訂單狀態
     */
    int updateStatus(@Param("id") Long id, @Param("statusId") Long statusId);
    
    /**
     * 根據ID刪除訂單
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 統計用戶訂單數量
     */
    long countByMemberId(@Param("memberId") Long memberId);
    
    /**
     * 統計總訂單數量
     */
    long count();
    
    /**
     * 生成唯一訂單號
     */
    boolean existsByOrderNumber(@Param("orderNumber") String orderNumber);
}