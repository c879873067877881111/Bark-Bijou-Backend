package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    private Long id;
    private Long memberId;
    private String orderNumber;
    private Long statusId;
    private Integer paymentId;
    private BigDecimal totalAmount;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private String shippingAddress;
    private String billingAddress;
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
    private LocalDateTime deletedAt;
}
