package com.smallnine.apiserver.service.impl;

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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl {
    
    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final ProductDao productDao;
    private final CartServiceImpl cartService;
    private final ProductServiceImpl productService;
    
    /**
     * 根據ID查詢訂單
     */
    public Order findById(Long id) {
        return orderDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
    }
    
    /**
     * 根據訂單號查詢訂單
     */
    public Order findByOrderNumber(String orderNumber) {
        return orderDao.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
    }
    
    /**
     * 查詢用戶訂單列表
     */
    public List<Order> findUserOrders(Long memberId, int page, int size) {
        int offset = page * size;
        return orderDao.findByMemberId(memberId, offset, size);
    }
    
    /**
     * 查詢訂單項目
     */
    public List<OrderItem> findOrderItems(Long orderId) {
        return orderItemDao.findByOrderId(orderId);
    }
    
    /**
     * 從購物車創建訂單
     */
    @Transactional
    public Order createOrderFromCart(Long memberId, String shippingAddress, String notes) {
        log.info("從購物車創建訂單: memberId={}", memberId);
        
        // 獲取購物車項目
        List<CartItem> cartItems = cartService.getCartItems(memberId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(400, "購物車為空，無法創建訂單");
        }
        
        // 驗證庫存
        if (!cartService.validateCartStock(memberId)) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK, "購物車中有商品庫存不足");
        }
        
        // 計算訂單金額
        BigDecimal totalAmount = cartService.calculateCartTotal(memberId);
        
        // 創建訂單
        Order order = new Order();
        order.setMemberId(memberId);
        order.setOrderNumber(generateOrderNumber());
        order.setStatusId(1L); // 假設1為"待處理"狀態
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
        
        // 創建訂單項目
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
        
        // 減少庫存
        for (CartItem cartItem : cartItems) {
            boolean stockDecreased = productService.decreaseStock(cartItem.getProductId(), cartItem.getQuantity());
            if (!stockDecreased) {
                throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK, 
                    "商品庫存不足，訂單創建失敗: productId=" + cartItem.getProductId());
            }
        }
        
        // 清空購物車
        cartService.clearCart(memberId);
        
        log.info("訂單創建完成: orderId={}, totalAmount={}", order.getId(), totalAmount);
        return order;
    }
    
    /**
     * 更新訂單狀態
     */
    @Transactional
    public void updateOrderStatus(Long orderId, Long statusId) {
        log.info("更新訂單狀態: orderId={}, statusId={}", orderId, statusId);
        
        Order order = findById(orderId);
        
        // 驗證狀態轉換是否合法（此處可以加入狀態機邏輯）
        if (order.getStatusId().equals(statusId)) {
            throw new BusinessException(400, "訂單狀態未發生變化");
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
        
        Order order = findById(orderId);
        
        // 驗證訂單屬於當前用戶
        if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "無權限操作此訂單");
        }
        
        // 檢查訂單狀態是否允許取消
        if (order.getStatusId() > 2L) { // 假設狀態ID > 2表示已處理，不能取消
            throw new BusinessException(ResponseCode.ORDER_STATUS_ERROR, "訂單已處理，無法取消");
        }
        
        // 恢復庫存
        List<OrderItem> orderItems = orderItemDao.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            Product product = productDao.findById(item.getProductId()).orElse(null);
            if (product != null) {
                int newStock = product.getStockQuantity() + item.getQuantity();
                productDao.updateStock(item.getProductId(), newStock);
            }
        }
        
        // 更新訂單狀態為已取消（假設6為已取消狀態）
        updateOrderStatus(orderId, 6L);
        
        log.info("訂單取消成功: orderId={}", orderId);
    }
    
    /**
     * 刪除訂單
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        log.info("刪除訂單: orderId={}", orderId);
        
        Order order = findById(orderId);
        
        // 先刪除訂單項目
        orderItemDao.deleteByOrderId(orderId);
        
        // 再刪除訂單
        orderDao.deleteById(orderId);
        
        log.info("訂單刪除成功: orderId={}", orderId);
    }
    
    /**
     * 統計用戶訂單數量
     */
    public long countUserOrders(Long memberId) {
        return orderDao.countByMemberId(memberId);
    }
    
    /**
     * 生成唯一訂單號
     */
    private String generateOrderNumber() {
        String prefix = "ORD";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        // 嘗試生成唯一訂單號
        for (int i = 1; i <= 10; i++) {
            String orderNumber = prefix + timestamp + String.format("%02d", i);
            if (!orderDao.existsByOrderNumber(orderNumber)) {
                return orderNumber;
            }
        }
        
        throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, "無法生成唯一訂單號");
    }
}