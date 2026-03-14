package com.smallnine.apiserver.dto;

import com.smallnine.apiserver.entity.Order;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private Long statusId;
    private Integer paymentId;
    private BigDecimal totalAmount;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
    private String deliveryMethod;
    private String city;
    private String town;
    private String address;
    private String storeName;
    private String storeAddress;
    private Integer couponId;
    private String discountType;
    private BigDecimal discountValue;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.orderNumber = order.getOrderNumber();
        this.statusId = order.getStatusId();
        this.paymentId = order.getPaymentId();
        this.totalAmount = order.getTotalAmount();
        this.shippingAmount = order.getShippingAmount();
        this.taxAmount = order.getTaxAmount();
        this.discountAmount = order.getDiscountAmount();
        this.shippingAddress = order.getShippingAddress();
        this.recipientName = order.getRecipientName();
        this.recipientPhone = order.getRecipientPhone();
        this.recipientEmail = order.getRecipientEmail();
        this.deliveryMethod = order.getDeliveryMethod();
        this.city = order.getCity();
        this.town = order.getTown();
        this.address = order.getAddress();
        this.storeName = order.getStoreName();
        this.storeAddress = order.getStoreAddress();
        this.couponId = order.getCouponId();
        this.discountType = order.getDiscountType();
        this.discountValue = order.getDiscountValue();
        this.notes = order.getNotes();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
    }

    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(order);
    }
}
