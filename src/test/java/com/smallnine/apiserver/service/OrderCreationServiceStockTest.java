package com.smallnine.apiserver.service;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.OrderDao;
import com.smallnine.apiserver.dao.OrderItemDao;
import com.smallnine.apiserver.dto.CreateOrderRequest;
import com.smallnine.apiserver.entity.CartItem;
import com.smallnine.apiserver.entity.Order;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.impl.OrderCreationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderCreationServiceImpl.create() 庫存防線測試。
 *
 * 重構重點（對應這支測試守護的兩個不變量）：
 * 1. 不再做 validateCartStock 前置軟檢查；conditional UPDATE 的 decreaseStock 回傳值
 *    本身就是唯一庫存防線（任何一顆扣失敗就 throw，@Transactional 整批 rollback）。
 * 2. 扣庫存前先按 productId 排序，避免兩張併發訂單以相反順序鎖同一批商品列而互鎖。
 */
@ExtendWith(MockitoExtension.class)
class OrderCreationServiceStockTest {

    @Mock private OrderDao orderDao;
    @Mock private OrderItemDao orderItemDao;
    @Mock private CartService cartService;
    @Mock private ProductService productService;

    private OrderCreationServiceImpl orderCreationService;

    private static final Long MEMBER_ID = 1L;
    private static final String IDEMPOTENCY_KEY = "idem-key-1";

    @BeforeEach
    void setUp() {
        orderCreationService = new OrderCreationServiceImpl(
                orderDao, orderItemDao, cartService, productService);
    }

    private CartItem cartItem(Long productId, int quantity) {
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal("100"));
        return item;
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
    void decreaseStock_alwaysInAscendingProductIdOrder() {
        // 購物車刻意亂序（3, 1, 2），扣庫存必須被排成 1 → 2 → 3，否則併發訂單會交叉鎖死
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(cartItem(3L, 1));
        cartItems.add(cartItem(1L, 1));
        cartItems.add(cartItem(2L, 1));

        when(cartService.getCartItems(MEMBER_ID)).thenReturn(cartItems);
        when(cartService.calculateCartTotal(MEMBER_ID)).thenReturn(new BigDecimal("300"));
        when(productService.decreaseStock(anyLong(), eq(1))).thenReturn(true);
        when(orderDao.existsByOrderNumber(any())).thenReturn(false);

        orderCreationService.create(MEMBER_ID, buildRequest(), IDEMPOTENCY_KEY);

        InOrder order = inOrder(productService);
        order.verify(productService).decreaseStock(1L, 1);
        order.verify(productService).decreaseStock(2L, 1);
        order.verify(productService).decreaseStock(3L, 1);
    }

    @Test
    void insufficientStock_throwsAndNeverInsertsOrder() {
        // 第二顆商品扣不到庫存 → 直接 throw、不可往下建單（真實環境靠 @Transactional rollback 把前面扣的還原）
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(cartItem(1L, 1));
        cartItems.add(cartItem(2L, 5));

        when(cartService.getCartItems(MEMBER_ID)).thenReturn(cartItems);
        when(cartService.calculateCartTotal(MEMBER_ID)).thenReturn(new BigDecimal("600"));
        when(productService.decreaseStock(1L, 1)).thenReturn(true);
        when(productService.decreaseStock(2L, 5)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderCreationService.create(MEMBER_ID, buildRequest(), IDEMPOTENCY_KEY));

        assertEquals(ResponseCode.INSUFFICIENT_STOCK.getCode(), ex.getCode());
        verify(orderDao, never()).insert(any(Order.class));
        verify(orderItemDao, never()).insertBatch(any());
        verify(cartService, never()).clearCart(anyLong());
    }

    @Test
    void emptyCart_throwsCartEmpty() {
        when(cartService.getCartItems(MEMBER_ID)).thenReturn(new ArrayList<>());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderCreationService.create(MEMBER_ID, buildRequest(), IDEMPOTENCY_KEY));

        assertEquals(ResponseCode.CART_EMPTY.getCode(), ex.getCode());
        verify(productService, never()).decreaseStock(anyLong(), any());
    }

    @Test
    void happyPath_insertsOrderAndItemsThenClearsCart() {
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(cartItem(1L, 2));
        cartItems.add(cartItem(2L, 1));

        when(cartService.getCartItems(MEMBER_ID)).thenReturn(cartItems);
        when(cartService.calculateCartTotal(MEMBER_ID)).thenReturn(new BigDecimal("300"));
        when(productService.decreaseStock(anyLong(), any())).thenReturn(true);
        when(orderDao.existsByOrderNumber(any())).thenReturn(false);

        Order created = orderCreationService.create(MEMBER_ID, buildRequest(), IDEMPOTENCY_KEY);

        assertEquals(MEMBER_ID, created.getMemberId());
        assertEquals(IDEMPOTENCY_KEY, created.getIdempotencyKey());
        verify(orderDao).insert(any(Order.class));

        InOrder order = inOrder(productService, orderDao, orderItemDao, cartService);
        order.verify(productService).decreaseStock(1L, 2);
        order.verify(productService).decreaseStock(2L, 1);
        order.verify(orderDao).insert(any(Order.class));
        order.verify(orderItemDao).insertBatch(any());
        order.verify(cartService).clearCart(MEMBER_ID);
    }
}