package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.OrderStatus;
import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.OrderDao;
import com.smallnine.apiserver.dao.OrderItemDao;
import com.smallnine.apiserver.dao.ProductDao;
import com.smallnine.apiserver.dto.CreateOrderRequest;
import com.smallnine.apiserver.entity.Order;
import com.smallnine.apiserver.entity.OrderItem;
import com.smallnine.apiserver.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.smallnine.apiserver.service.OrderCreationService;
import com.smallnine.apiserver.service.OrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final ProductDao productDao;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderCreationService orderCreationService;

    /**
     * 根據ID查詢訂單（內部使用，無授權檢查）
     */
    private Order findByIdInternal(Long id) {
        return orderDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
    }

    /**
     * 根據ID查詢訂單（帶授權檢查）
     */
    public Order findById(Long id, Long memberId) {
        Order order = findByIdInternal(id);
        validateOrderOwnership(order, memberId);
        return order;
    }

    /**
     * 根據訂單號查詢訂單（帶授權檢查）
     */
    public Order findByOrderNumber(String orderNumber, Long memberId) {
        Order order = orderDao.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
        validateOrderOwnership(order, memberId);
        return order;
    }

    /**
     * 查詢用戶訂單列表
     */
    public List<Order> findUserOrders(Long memberId, int page, int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BusinessException(ResponseCode.INVALID_PAGINATION);
        }
        int offset = page * size;
        return orderDao.findByMemberId(memberId, offset, size);
    }

    /**
     * 查詢訂單項目（帶授權檢查）
     */
    public List<OrderItem> findOrderItems(Long orderId, Long memberId) {
        Order order = findByIdInternal(orderId);
        validateOrderOwnership(order, memberId);
        return orderItemDao.findByOrderId(orderId);
    }

    /**
     * 驗證訂單所有權
     */
    private void validateOrderOwnership(Order order, Long memberId) {
        if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "無權限操作此訂單");
        }
    }

    /**
     * 從購物車創建訂單（無冪等鍵）
     */
    @Override
    public Order createOrderFromCart(Long memberId, CreateOrderRequest request) {
        return orderCreationService.create(memberId, request, null);
    }

    @Override
    public Order createOrderFromCart(Long memberId, CreateOrderRequest request, String idempotencyKey) {
        if (idempotencyKey == null) {
            return orderCreationService.create(memberId, request, null);
        }

        String redisKey = "order:idempotency:" + memberId + ":" + idempotencyKey;

        // SETNX 原子佔位，防止並行請求同時通過（Redis 快路徑）
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PENDING", 24, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(acquired)) {
            // 鍵已存在，查回已建立的訂單
            Object existingOrderId = redisTemplate.opsForValue().get(redisKey);
            if (existingOrderId != null && !"PENDING".equals(existingOrderId.toString())) {
                return orderDao.findById(Long.parseLong(existingOrderId.toString()))
                        .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
            }
            throw new BusinessException(ResponseCode.CONFLICT, "訂單正在建立中，請稍後重試");
        }

        Order order;
        try {
            // 透過獨立 bean 呼叫，讓 @Transactional 切面真正啟動獨立交易
            // DB commit 完成後才會回到這裡，避免 Redis 寫了 orderId 但 DB rollback
            order = orderCreationService.create(memberId, request, idempotencyKey);
        } catch (DuplicateKeyException dup) {
            // DB unique constraint 兜底：Redis 失效時 retry 撞到 (member_id, idempotency_key) unique。
            // 表示同鍵的訂單已被先前一次 commit 寫入；查回那筆給呼叫者，整筆交易已 rollback、不會多扣庫存。
            log.warn("idempotency=db_unique_fallback key={} memberId={}", redisKey, memberId);
            order = orderDao.findByMemberAndIdempotencyKey(memberId, idempotencyKey)
                    .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
        } catch (Exception e) {
            // 其他例外：Redis 清除若失敗，記 warn 但保留原始業務例外（不被 cleanup 例外蓋掉）
            try {
                redisTemplate.delete(redisKey);
            } catch (Exception cleanup) {
                log.warn("idempotency=cleanup_failed key={} reason={}", redisKey, cleanup.getMessage());
            }
            throw e;
        }

        // DB 已 commit，訂單建立成功；Redis 收尾是 best-effort。
        // PENDING→orderId 寫入失敗時嘗試 delete(key) 自癒：留著 PENDING 會讓
        // 24h 內 retry 永遠 CONFLICT。TTL 過後 retry 也不會重複下單——DB unique constraint 會擋下。
        try {
            redisTemplate.opsForValue().set(redisKey, order.getId().toString(), 24, TimeUnit.HOURS);
        } catch (Exception finalizeEx) {
            try {
                redisTemplate.delete(redisKey);
            } catch (Exception cleanup) {
                log.warn("idempotency=cleanup_failed key={} reason={}", redisKey, cleanup.getMessage());
            }
            log.error("idempotency=finalize_failed key={} orderId={} reason={}",
                    redisKey, order.getId(), finalizeEx.getMessage());
        }
        return order;
    }

    /**
     * 更新訂單狀態
     */
    @Transactional
    public void updateOrderStatus(Long orderId, Long statusId) {
        log.info("更新訂單狀態: orderId={}, statusId={}", orderId, statusId);

        Order order = findByIdInternal(orderId);

        if (order.getStatusId().equals(statusId)) {
            throw new BusinessException(ResponseCode.ORDER_STATUS_ERROR, "訂單狀態未發生變化");
        }

        OrderStatus currentStatus;
        OrderStatus newStatus;
        try {
            currentStatus = OrderStatus.fromId(order.getStatusId());
            newStatus = OrderStatus.fromId(statusId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResponseCode.ORDER_STATUS_ERROR, "無效的訂單狀態");
        }

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new BusinessException(ResponseCode.ORDER_INVALID_TRANSITION,
                    "無法從 " + currentStatus.getDescription() + " 轉換到 " + newStatus.getDescription());
        }

        int updatedRows = orderDao.updateStatus(orderId, statusId);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.ORDER_NOT_FOUND);
        }

        log.info("訂單狀態更新成功: orderId={}, oldStatus={}, newStatus={}",
                 orderId, order.getStatusId(), statusId);
    }

    /**
     * 取消訂單
     */
    @Transactional
    public void cancelOrder(Long orderId, Long memberId) {
        log.info("取消訂單: orderId={}, memberId={}", orderId, memberId);

        Order order = findByIdInternal(orderId);
        validateOrderOwnership(order, memberId);

        // #C3：先用 CAS 一步把狀態翻成已取消（狀態仍可取消才會更新到行）。
        // 並發下只有唯一勝出的 thread 拿到 updated=1，敗者拿 0、直接回明確訊息，
        // 不再走「先還庫存→updateOrderStatus 撞狀態→整筆 rollback」那套浪費寫入 + 誤導訊息。
        int updated = orderDao.cancelIfCancellable(
                orderId, OrderStatus.CANCELLED.getId(), OrderStatus.cancellableStatusIds());
        if (updated == 0) {
            throw new BusinessException(ResponseCode.ORDER_STATUS_ERROR,
                    "訂單已取消，或目前狀態無法取消");
        }

        // 確認唯一勝出後才還庫存（原子操作）
        List<OrderItem> orderItems = orderItemDao.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            productDao.increaseStock(item.getProductId(), item.getQuantity());
        }

        log.info("訂單取消成功: orderId={}", orderId);
    }

    /**
     * 刪除訂單（帶授權檢查）
     */
    @Transactional
    public void deleteOrder(Long orderId, Long memberId) {
        log.info("刪除訂單: orderId={}, memberId={}", orderId, memberId);

        Order order = findByIdInternal(orderId);
        validateOrderOwnership(order, memberId);

        // 只允許刪除已取消的訂單
        OrderStatus currentStatus = OrderStatus.fromId(order.getStatusId());
        if (currentStatus != OrderStatus.CANCELLED) {
            throw new BusinessException(ResponseCode.ORDER_STATUS_ERROR, "只能刪除已取消的訂單");
        }

        // 先刪除訂單項目
        orderItemDao.deleteByOrderId(orderId);

        // 再刪除訂單
        orderDao.deleteById(orderId);

        log.info("訂單刪除成功: orderId={}", orderId);
    }

    /**
     * 刪除訂單（管理員專用，無授權檢查）
     */
    @Transactional
    public void deleteOrderAsAdmin(Long orderId) {
        log.info("管理員刪除訂單: orderId={}", orderId);

        Order order = findByIdInternal(orderId);

        // 只允許刪除已取消的訂單
        OrderStatus currentStatus = OrderStatus.fromId(order.getStatusId());
        if (currentStatus != OrderStatus.CANCELLED) {
            throw new BusinessException(ResponseCode.ORDER_STATUS_ERROR, "只能刪除已取消的訂單");
        }

        orderItemDao.deleteByOrderId(orderId);
        orderDao.deleteById(orderId);

        log.info("管理員訂單刪除成功: orderId={}", orderId);
    }

    /**
     * 統計用戶訂單數量
     */
    public long countUserOrders(Long memberId) {
        return orderDao.countByMemberId(memberId);
    }
}