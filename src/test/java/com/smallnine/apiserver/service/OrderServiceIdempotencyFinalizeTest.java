package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dao.OrderDao;
import com.smallnine.apiserver.dao.OrderItemDao;
import com.smallnine.apiserver.dao.ProductDao;
import com.smallnine.apiserver.dto.CreateOrderRequest;
import com.smallnine.apiserver.entity.Order;
import com.smallnine.apiserver.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * #C4-revised：DB 訂單已 commit 後，冪等鍵 PENDING→orderId 的最終寫入若失敗，
 * 不可把 key 留在 PENDING（否則 24h 內 retry 永遠 CONFLICT、TTL 過後又重複下單）。
 *
 * 純單元測試：直接 mock OrderServiceImpl 的依賴，OrderCreationService 回傳已建好的訂單，
 * 讓最終 set 拋例外，驗證會 delete(key) 自癒、且業務本身仍回傳訂單。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceIdempotencyFinalizeTest {

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
    void finalizeSetFails_deletesKeyAndStillReturnsOrder() {
        String idempotencyKey = UUID.randomUUID().toString();
        String redisKey = "order:idempotency:" + MEMBER_ID + ":" + idempotencyKey;

        Order created = new Order();
        created.setId(999L);

        // OrderCreationService 回傳一個建單成功的訂單（模擬 DB 已 commit）
        when(orderCreationService.create(eq(MEMBER_ID), any(CreateOrderRequest.class), eq(idempotencyKey)))
                .thenReturn(created);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // SETNX 佔位成功（拿到鎖），但最終 PENDING→orderId 的 set 炸掉
        when(valueOps.setIfAbsent(eq(redisKey), eq("PENDING"), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        doThrow(new RuntimeException("redis down"))
                .when(valueOps).set(eq(redisKey), anyString(), anyLong(), any(TimeUnit.class));

        // 業務本身必須成功：訂單已建立並回傳，Redis 收尾是 best-effort
        Order order = orderService.createOrderFromCart(MEMBER_ID, buildRequest(), idempotencyKey);

        assertNotNull(order);
        assertEquals(999L, order.getId());

        // 關鍵：set 失敗後必須 delete(key)，不可把 key 留在 PENDING
        verify(redisTemplate).delete(redisKey);
    }

    @Test
    void redisFullyDown_setAndDeleteBothThrow_stillReturnsOrderNoException() {
        String idempotencyKey = UUID.randomUUID().toString();
        String redisKey = "order:idempotency:" + MEMBER_ID + ":" + idempotencyKey;

        Order created = new Order();
        created.setId(888L);

        when(orderCreationService.create(eq(MEMBER_ID), any(CreateOrderRequest.class), eq(idempotencyKey)))
                .thenReturn(created);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(redisKey), eq("PENDING"), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        // Redis 全掛：set 與收尾的 delete 都炸（最真實的相關性故障）
        doThrow(new RuntimeException("redis down"))
                .when(valueOps).set(eq(redisKey), anyString(), anyLong(), any(TimeUnit.class));
        when(redisTemplate.delete(redisKey)).thenThrow(new RuntimeException("redis down"));

        // 內層 catch 必須吞住 delete 例外，不可往外噴；訂單仍回傳（best-effort）
        Order order = assertDoesNotThrow(() ->
                orderService.createOrderFromCart(MEMBER_ID, buildRequest(), idempotencyKey));

        assertNotNull(order);
        assertEquals(888L, order.getId());
        verify(redisTemplate).delete(redisKey);
    }
}