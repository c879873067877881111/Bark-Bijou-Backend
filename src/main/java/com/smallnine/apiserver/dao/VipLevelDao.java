package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.VipLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Mapper
public interface VipLevelDao {
    
    /**
     * 根據ID查詢VIP等級
     */
    Optional<VipLevel> findById(@Param("id") Long id);
    
    /**
     * 根據名稱查詢VIP等級
     */
    Optional<VipLevel> findByName(@Param("name") String name);
    
    /**
     * 查詢所有啟用的VIP等級（按排序）
     */
    List<VipLevel> findAllActive();
    
    /**
     * 查詢所有VIP等級
     */
    List<VipLevel> findAll();
    
    /**
     * 根據消費金額查詢匹配的VIP等級
     */
    Optional<VipLevel> findBySpending(@Param("spending") BigDecimal spending);
    
    /**
     * 創建VIP等級
     */
    int insert(VipLevel vipLevel);
    
    /**
     * 更新VIP等級
     */
    int update(VipLevel vipLevel);
    
    /**
     * 刪除VIP等級
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 檢查名稱是否已存在
     */
    boolean existsByName(@Param("name") String name);
    
    /**
     * 統計VIP等級總數
     */
    long count();
    
    /**
     * 統計啟用的VIP等級數量
     */
    long countActive();
}