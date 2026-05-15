package com.smallnine.apiserver.service;

import com.smallnine.apiserver.constants.enums.OrderStatus;
import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.CartItemDao;
import com.smallnine.apiserver.dao.OrderDao;
import com.smallnine.apiserver.dao.OrderItemDao;
import com.smallnine.apiserver.dao.ProductDao;
import com.smallnine.apiserver.dto.CreateOrderRequest;
import com.smallnine.apiserver.entity.Order;
import com.smallnine.apiserver.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderDao orderDao;
    @Autowired private OrderItemDao orderItemDao;
    @Autowired private CartService cartService;
    @Autowired private CartItemDao cartItemDao;
    @Autowired private ProductDao productDao;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final Long PRODUCT_ID = 1L;

    private Long pendingOrderId;

    private CreateOrderRequest buildRequest(String addr) {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setRecipientName("測試用戶");
        req.setRecipientPhone("0912345678");
        req.setDeliveryMethod("HOME_DELIVERY");
        req.setShippingAddress(addr);
        return req;
    }

    @BeforeEach
    void setUp() {
        // 清空購物車，加入商品
        cartService.clearCart(MEMBER_ID);
        cartService.addToCart(MEMBER_ID, PRODUCT_ID, 2);

        // 建立一筆 PENDING 訂單供狀態機測試
        Order order = new Order();
        order.setMemberId(MEMBER_ID);
        order.setOrderNumber("TEST-" + UUID.randomUUID().toString().substring(0, 8));
        order.setStatusId(OrderStatus.PENDING.getId());
        order.setTotalAmount(new BigDecimal("1598.00"));
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingAddress("test address");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderDao.insert(order);
        pendingOrderId = order.getId();
    }

    @AfterEach
    void tearDown() {
        // 清 Redis idempotency keys
        Set<String> keys = redisTemplate.keys("order:idempotency:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ── 狀態轉換 ──

    @Test
    void updateOrderStatus_validTransition() {
        orderService.updateOrderStatus(pendingOrderId, OrderStatus.CONFIRMED.getId());

        Order updated = orderDao.findById(pendingOrderId).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED.getId(), updated.getStatusId());
    }

    @Test
    void updateOrderStatus_invalidTransition() {
        // 先把訂單改成 CANCELLED
        orderDao.updateStatus(pendingOrderId, OrderStatus.CANCELLED.getId());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.updateOrderStatus(pendingOrderId, OrderStatus.PENDING.getId()));
        assertEquals(ResponseCode.ORDER_INVALID_TRANSITION.getCode(), ex.getCode());
    }

    @Test
    void updateOrderStatus_sameStatus() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.updateOrderStatus(pendingOrderId, OrderStatus.PENDING.getId()));
        assertEquals(ResponseCode.ORDER_STATUS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void updateOrderStatus_invalidStatusId() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.updateOrderStatus(pendingOrderId, 999L));
        assertEquals(ResponseCode.ORDER_STATUS_ERROR.getCode(), ex.getCode());
    }

    // ── 取消訂單 ──

    @Test
    void cancelOrder_pending() {
        // 先建一筆有 order_items 的真實訂單（透過購物車結帳）
        Order created = orderService.createOrderFromCart(MEMBER_ID, buildRequest("addr"), null);

        int stockBeforeCancel = productDao.findById(PRODUCT_ID).orElseThrow().getStockQuantity();

        orderService.cancelOrder(created.getId(), MEMBER_ID);

        Order cancelled = orderDao.findById(created.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED.getId(), cancelled.getStatusId());

        int stockAfterCancel = productDao.findById(PRODUCT_ID).orElseThrow().getStockQuantity();
        assertEquals(stockBeforeCancel + 2, stockAfterCancel, "庫存應恢復購買數量");
    }

    @Test
    void cancelOrder_shipped() {
        orderDao.updateStatus(pendingOrderId, OrderStatus.SHIPPED.getId());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.cancelOrder(pendingOrderId, MEMBER_ID));
        assertEquals(ResponseCode.ORDER_STATUS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void cancelOrder_notOwner() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.cancelOrder(pendingOrderId, OTHER_MEMBER_ID));
        assertEquals(ResponseCode.FORBIDDEN.getCode(), ex.getCode());
    }

    // ── #C3 CAS 取消：併發競態收斂 ──

    @Test
    void cancelOrder_alreadyCancelled_noDoubleStockRestore() {
        // 第一次取消：成功，庫存還回
        Order created = orderService.createOrderFromCart(MEMBER_ID, buildRequest("addr"), null);
        orderService.cancelOrder(created.getId(), MEMBER_ID);

        int stockAfterFirstCancel = productDao.findById(PRODUCT_ID).orElseThrow().getStockQuantity();

        // 第二次取消（模擬併發敗者）：CAS 撞 0 行，直接回明確訊息，且不可再還一次庫存
        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.cancelOrder(created.getId(), MEMBER_ID));
        assertEquals(ResponseCode.ORDER_STATUS_ERROR.getCode(), ex.getCode());

        int stockAfterSecondCancel = productDao.findById(PRODUCT_ID).orElseThrow().getStockQuantity();
        assertEquals(stockAfterFirstCancel, stockAfterSecondCancel, "敗者不可重複還庫存");
    }

    @Test
    void cancelIfCancellable_firstWinsSecondLoses() {
        // 第一次：狀態仍可取消 → 更新到 1 行（唯一勝者）
        int firstWin = orderDao.cancelIfCancellable(
                pendingOrderId, OrderStatus.CANCELLED.getId(), OrderStatus.cancellableStatusIds());
        assertEquals(1, firstWin, "可取消狀態應更新到 1 行");

        // 第二次：已是 CANCELLED → 更新到 0 行（敗者）
        int secondLose = orderDao.cancelIfCancellable(
                pendingOrderId, OrderStatus.CANCELLED.getId(), OrderStatus.cancellableStatusIds());
        assertEquals(0, secondLose, "已取消狀態應更新到 0 行");
    }

    @Test
    void cancelIfCancellable_nonCancellableStatusReturnsZero() {
        orderDao.updateStatus(pendingOrderId, OrderStatus.SHIPPED.getId());

        int affected = orderDao.cancelIfCancellable(
                pendingOrderId, OrderStatus.CANCELLED.getId(), OrderStatus.cancellableStatusIds());
        assertEquals(0, affected, "不可取消狀態 CAS 應更新到 0 行");

        Order stillShipped = orderDao.findById(pendingOrderId).orElseThrow();
        assertEquals(OrderStatus.SHIPPED.getId(), stillShipped.getStatusId(), "狀態不可被誤翻成已取消");
    }

    @Test
    void cancellableStatusIds_isPendingAndConfirmed() {
        // 鎖住單一真相來源內容，狀態機被誤改時這裡先紅
        List<Long> ids = OrderStatus.cancellableStatusIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains(OrderStatus.PENDING.getId()));
        assertTrue(ids.contains(OrderStatus.CONFIRMED.getId()));
    }

    @Test
    void cancelIfCancellable_emptyFromStatusIds_updatesZeroRows() {
        // 空集合防禦：絕不可退化成只用 id 更新任意狀態
        int affected = orderDao.cancelIfCancellable(
                pendingOrderId, OrderStatus.CANCELLED.getId(), new ArrayList<>());
        assertEquals(0, affected, "空可取消集合 CAS 應更新到 0 行");

        Order stillPending = orderDao.findById(pendingOrderId).orElseThrow();
        assertEquals(OrderStatus.PENDING.getId(), stillPending.getStatusId(), "空集合不可誤翻狀態");
    }

    // ── 從購物車建立訂單 ──

    @Test
    void createOrderFromCart_success() {
        int stockBefore = productDao.findById(PRODUCT_ID).orElseThrow().getStockQuantity();

        CreateOrderRequest req = buildRequest("shipping addr");
        req.setNotes("notes");
        Order order = orderService.createOrderFromCart(MEMBER_ID, req, null);

        assertNotNull(order.getId());
        assertNotNull(order.getOrderNumber());
        assertEquals(OrderStatus.PENDING.getId(), order.getStatusId());
        assertTrue(order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);

        // 購物車已清空
        assertTrue(cartService.getCartItems(MEMBER_ID).isEmpty());

        // 庫存已扣減
        int stockAfter = productDao.findById(PRODUCT_ID).orElseThrow().getStockQuantity();
        assertEquals(stockBefore - 2, stockAfter);
    }

    @Test
    void createOrderFromCart_emptyCart() {
        cartService.clearCart(MEMBER_ID);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.createOrderFromCart(MEMBER_ID, buildRequest("addr"), null));
        assertEquals(ResponseCode.CART_EMPTY.getCode(), ex.getCode());
    }

    // ── 冪等性 ──

    @Test
    void createOrderFromCart_idempotent_firstCall() {
        String idempotencyKey = UUID.randomUUID().toString();

        Order order = orderService.createOrderFromCart(MEMBER_ID, buildRequest("addr"), idempotencyKey);

        assertNotNull(order.getId());

        String redisKey = "order:idempotency:" + MEMBER_ID + ":" + idempotencyKey;
        Object storedValue = redisTemplate.opsForValue().get(redisKey);
        assertEquals(order.getId().toString(), storedValue.toString());
    }

    @Test
    void createOrderFromCart_idempotent_duplicate() {
        String idempotencyKey = UUID.randomUUID().toString();

        Order first = orderService.createOrderFromCart(MEMBER_ID, buildRequest("addr"), idempotencyKey);

        // 再加一些東西到購物車（模擬第二次請求）
        cartService.addToCart(MEMBER_ID, PRODUCT_ID, 1);

        Order second = orderService.createOrderFromCart(MEMBER_ID, buildRequest("addr"), idempotencyKey);

        assertEquals(first.getId(), second.getId());
    }

    // ── 軟刪除 ──

    @Test
    void softDelete_orderNotVisible() {
        // 先取消才能刪除
        orderDao.updateStatus(pendingOrderId, OrderStatus.CANCELLED.getId());

        orderService.deleteOrder(pendingOrderId, MEMBER_ID);

        Optional<Order> found = orderDao.findById(pendingOrderId);
        assertTrue(found.isEmpty(), "軟刪除後 findById 應查不到");
    }
}
