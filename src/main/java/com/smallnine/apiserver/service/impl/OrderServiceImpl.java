package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.OrderStatus;
import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.OrderDao;
import com.smallnine.apiserver.dao.OrderItemDao;
import com.smallnine.apiserver.dao.ProductDao;
import com.smallnine.apiserver.entity.CartItem;
import com.smallnine.apiserver.entity.Order;
import com.smallnine.apiserver.entity.OrderItem;
import com.smallnine.apiserver.entity.Product;
import com.smallnine.apiserver.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.smallnine.apiserver.service.CartService;
import com.smallnine.apiserver.service.OrderService;
import com.smallnine.apiserver.service.ProductService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final ProductDao productDao;
    private final CartService cartService;
    private final ProductService productService;
    private final RedisTemplate<String, Object> redisTemplate;
    
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
     * 從購物車創建訂單
     */
    @Transactional
    public Order createOrderFromCart(Long memberId, String shippingAddress, String notes) {
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
        order.setShippingAddress(shippingAddress);
        order.setNotes(notes);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

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

    @Override
    @Transactional
    public Order createOrderFromCart(Long memberId, String shippingAddress, String notes, String idempotencyKey) {
        if (idempotencyKey == null) {
            return createOrderFromCart(memberId, shippingAddress, notes);
        }

        String redisKey = "order:idempotency:" + memberId + ":" + idempotencyKey;

        // SETNX 原子佔位，防止並行請求同時通過
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

        try {
            Order order = createOrderFromCart(memberId, shippingAddress, notes);
            // 建單成功，將 PENDING 替換成實際訂單 ID
            redisTemplate.opsForValue().set(redisKey, order.getId().toString(), 24, TimeUnit.HOURS);
            return order;
        } catch (Exception e) {
            // 建單失敗，釋放佔位鍵
            redisTemplate.delete(redisKey);
            throw e;
        }
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

        // 檢查訂單狀態是否允許取消
        OrderStatus currentStatus = OrderStatus.fromId(order.getStatusId());
        if (!currentStatus.canCancel()) {
            throw new BusinessException(ResponseCode.ORDER_STATUS_ERROR, "訂單狀態為「" + currentStatus.getDescription() + "」，無法取消");
        }

        // 恢復庫存（原子操作）
        List<OrderItem> orderItems = orderItemDao.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            productDao.increaseStock(item.getProductId(), item.getQuantity());
        }

        // 更新訂單狀態為已取消
        updateOrderStatus(orderId, OrderStatus.CANCELLED.getId());

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