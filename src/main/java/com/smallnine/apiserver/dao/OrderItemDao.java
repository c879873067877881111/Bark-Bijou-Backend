package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface OrderItemDao {
    
    /**
     * 根據ID查詢訂單項目
     */
    Optional<OrderItem> findById(@Param("id") Long id);
    
    /**
     * 根據訂單ID查詢訂單項目列表
     */
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);
    
    /**
     * 創建訂單項目
     */
    int insert(OrderItem orderItem);
    
    /**
     * 批量創建訂單項目
     */
    int insertBatch(@Param("orderItems") List<OrderItem> orderItems);
    
    /**
     * 更新訂單項目
     */
    int update(OrderItem orderItem);
    
    /**
     * 根據ID刪除訂單項目
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根據訂單ID刪除所有訂單項目
     */
    int deleteByOrderId(@Param("orderId") Long orderId);
    
    /**
     * 統計訂單項目數量
     */
    long countByOrderId(@Param("orderId") Long orderId);
}