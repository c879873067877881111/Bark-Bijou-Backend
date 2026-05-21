package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dao.OrderDao;
import com.smallnine.apiserver.dao.OrderItemDao;
import com.smallnine.apiserver.dao.ProductDao;
import com.smallnine.apiserver.dto.CreateOrderRequest;
import com.smallnine.apiserver.entity.Order;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DB unique constraint 兜底：Redis 失效（Redis 全掛 / finalize 失敗導致 key 被 delete）後 retry，
 * 同一個 (memberId, idempotencyKey) 第二次寫 orders 會撞到 partial unique index，
 * 由 MyBatis-Spring 翻譯成 DuplicateKeyException。OrderServiceImpl 必須攔下這個例外、
 * 用 findByMemberAndIdempotencyKey 查回既有訂單，而不是把 5xx 噴回給呼叫者。
 *
 * 純單元測試：mock orderCreationService.create() 拋 DuplicateKeyException 模擬 retry 情境。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceDbUniqueFallbackTest {

    @Mock private OrderDao orderDao;
    @Mock private OrderItemDao orderItemDao;
    @Mock private ProductDao productDao;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private OrderCreationService orderCreationService;
    @Mock private ValueOperations<String, Object> valueOps;

    private OrderServiceImpl orderService;

    private static final Long MEMBER_ID = 1L;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderDao, orderItemDao, productDao,
                redisTemplate, orderCreationService);
    }

    private CreateOrderRequest buildRequest() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setRecipientName("測試用戶");
        req.setRecipientPhone("0912345678");
        req.setDeliveryMethod("HOME_DELIVERY");
        req.setShippingAddress("test address");
        return req;
    }

    @Test
    void retryAfterRedisFailure_dbUniqueViolation_returnsExistingOrder() {
        // 情境：前一次建單 DB commit 成功但 Redis finalize 失敗 → delete(key) → retry 進來
        String idempotencyKey = UUID.randomUUID().toString();
        String redisKey = "order:idempotency:" + MEMBER_ID + ":" + idempotencyKey;

        Order existingOrder = new Order();
        existingOrder.setId(777L);
        existingOrder.setIdempotencyKey(idempotencyKey);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // SETNX 拿到鎖（前一次的 key 已被 delete 掉，這次重新 SETNX 成功）
        when(valueOps.setIfAbsent(eq(redisKey), eq("PENDING"), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        // 建單時 DB 撞 partial unique index：(member_id, idempotency_key) 已存在
        when(orderCreationService.create(eq(MEMBER_ID), any(CreateOrderRequest.class), eq(idempotencyKey)))
                .thenThrow(new DuplicateKeyException("duplicate key value violates unique constraint"));
        // findByMemberAndIdempotencyKey 查回前一次已 commit 的訂單
        when(orderDao.findByMemberAndIdempotencyKey(MEMBER_ID, idempotencyKey))
                .thenReturn(Optional.of(existingOrder));

        Order returned = orderService.createOrderFromCart(MEMBER_ID, buildRequest(), idempotencyKey);

        assertNotNull(returned);
        assertEquals(777L, returned.getId(), "必須回傳前一次已建立的訂單，不可重複下單");

        verify(orderDao).findByMemberAndIdempotencyKey(MEMBER_ID, idempotencyKey);
        // unique violation 路徑下，cleanup 不可亂刪 key（這次 retry 還沒成功 finalize 之前 key 仍是 PENDING）
        verify(redisTemplate, never()).delete(redisKey);
    }

    @Test
    void retryAfterRedisFailure_dbUniqueButQueryNotFound_throwsOrderNotFound() {
        // 邊界情境：DB 拋 unique violation，但隨後查回時找不到（理論上不該發生，
        // 但 race condition / 軟刪除 / 資料庫複本延遲都可能造成）。
        // 這種狀況不可吞掉，必須拋 ORDER_NOT_FOUND 讓呼叫者明確失敗，不能誤回 null。
        String idempotencyKey = UUID.randomUUID().toString();
        String redisKey = "order:idempotency:" + MEMBER_ID + ":" + idempotencyKey;

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(redisKey), eq("PENDING"), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(orderCreationService.create(eq(MEMBER_ID), any(CreateOrderRequest.class), eq(idempotencyKey)))
                .thenThrow(new DuplicateKeyException("duplicate key"));
        when(orderDao.findByMemberAndIdempotencyKey(MEMBER_ID, idempotencyKey))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () ->
                orderService.createOrderFromCart(MEMBER_ID, buildRequest(), idempotencyKey));
    }
}
