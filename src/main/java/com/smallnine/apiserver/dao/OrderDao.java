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
     * CAS 取消：只有當訂單目前狀態仍屬可取消狀態時，才翻成已取消。
     * 並發下只有一個 thread 會更新到 1 行，其餘 0 行——把競態收斂成唯一勝者。
     *
     * @return 受影響行數（1=本次取消成功；0=已被取消或目前狀態不可取消）
     */
    int cancelIfCancellable(@Param("id") Long id,
                            @Param("cancelledStatusId") Long cancelledStatusId,
                            @Param("fromStatusIds") List<Long> fromStatusIds);
    
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

    /**
     * 冪等性兜底：用 (member_id, idempotency_key) 查回 Redis 失敗後 DB unique 攔下的訂單
     */
    Optional<Order> findByMemberAndIdempotencyKey(@Param("memberId") Long memberId,
                                                  @Param("idempotencyKey") String idempotencyKey);
}