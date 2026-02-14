package com.smallnine.apiserver.service;

import com.smallnine.apiserver.entity.Order;
import com.smallnine.apiserver.entity.OrderItem;

import java.util.List;

public interface OrderService {

    Order findById(Long id, Long memberId);

    Order findByOrderNumber(String orderNumber, Long memberId);

    List<Order> findUserOrders(Long memberId, int page, int size);

    List<OrderItem> findOrderItems(Long orderId, Long memberId);

    Order createOrderFromCart(Long memberId, String shippingAddress, String notes);

    Order createOrderFromCart(Long memberId, String shippingAddress, String notes, String idempotencyKey);

    void updateOrderStatus(Long orderId, Long statusId);

    void cancelOrder(Long orderId, Long memberId);

    void deleteOrder(Long orderId, Long memberId);

    void deleteOrderAsAdmin(Long orderId);

    long countUserOrders(Long memberId);
}
