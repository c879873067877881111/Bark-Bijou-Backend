package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.OrderStatus;
import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.OrderDao;
import com.smallnine.apiserver.dao.OrderItemDao;
import com.smallnine.apiserver.dto.CreateOrderRequest;
import com.smallnine.apiserver.entity.CartItem;
import com.smallnine.apiserver.entity.Order;
import com.smallnine.apiserver.entity.OrderItem;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.CartService;
import com.smallnine.apiserver.service.OrderCreationService;
import com.smallnine.apiserver.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreationServiceImpl implements OrderCreationService {

    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final CartService cartService;
    private final ProductService productService;

    /**
     * 從購物車建立訂單（單一交易單元）
     */
    @Override
    @Transactional
    public Order create(Long memberId, CreateOrderRequest request, String idempotencyKey) {
        log.info("從購物車創建訂單: memberId={}", memberId);

        // 1. 獲取購物車項目
        List<CartItem> cartItems = cartService.getCartItems(memberId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(ResponseCode.CART_EMPTY, "購物車為空，無法創建訂單");
        }

        // 2. 驗證庫存
        if (!cartService.validateCartStock(memberId)) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK, "購物車中有商品庫存不足");
        }

        // 3. 計算訂單金額
        BigDecimal totalAmount = cartService.calculateCartTotal(memberId);

        // 4. 先扣減所有庫存（原子 SQL，如果任何步驟失敗整個 @Transactional 會 rollback）
        for (CartItem cartItem : cartItems) {
            boolean stockDecreased = productService.decreaseStock(cartItem.getProductId(), cartItem.getQuantity());
            if (!stockDecreased) {
                throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK,
                    "商品庫存不足，訂單創建失敗: productId=" + cartItem.getProductId());
            }
        }

        // 5. 創建訂單
        Order order = new Order();
        order.setMemberId(memberId);
        order.setOrderNumber(generateOrderNumber());
        order.setStatusId(OrderStatus.PENDING.getId());
        order.setTotalAmount(totalAmount);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingAddress(request.getShippingAddress());
        order.setRecipientName(request.getRecipientName());
        order.setRecipientPhone(request.getRecipientPhone());
        order.setRecipientEmail(request.getRecipientEmail());
        order.setDeliveryMethod(request.getDeliveryMethod());
        order.setCity(request.getCity());
        order.setTown(request.getTown());
        order.setAddress(request.getAddress());
        order.setStoreName(request.getStoreName());
        order.setStoreAddress(request.getStoreAddress());
        order.setCouponId(request.getCouponId());
        order.setNotes(request.getNotes());
        // 冪等鍵寫進 DB，配合 partial unique index 兜底；Redis 失效時 retry 會撞 unique violation
        order.setIdempotencyKey(idempotencyKey);

        orderDao.insert(order);
        log.info("訂單創建成功: orderId={}, orderNumber={}", order.getId(), order.getOrderNumber());

        // 6. 創建訂單項目
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setTotalPrice(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItem.setCreatedAt(LocalDateTime.now());
            orderItems.add(orderItem);
        }

        if (!orderItems.isEmpty()) {
            orderItemDao.insertBatch(orderItems);
            log.info("訂單項目創建成功: 數量={}", orderItems.size());
        }

        // 7. 清空購物車
        cartService.clearCart(memberId);

        log.info("訂單創建完成: orderId={}, totalAmount={}", order.getId(), totalAmount);
        return order;
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ORDER_NUMBER_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /**
     * 生成唯一訂單號（不可預測）
     */
    private String generateOrderNumber() {
        String prefix = "ORD";

        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(prefix);
            for (int i = 0; i < 12; i++) {
                int index = SECURE_RANDOM.nextInt(ORDER_NUMBER_CHARS.length());
                sb.append(ORDER_NUMBER_CHARS.charAt(index));
            }
            String orderNumber = sb.toString();
            if (!orderDao.existsByOrderNumber(orderNumber)) {
                return orderNumber;
            }
        }

        throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, "無法生成唯一訂單號");
    }
}