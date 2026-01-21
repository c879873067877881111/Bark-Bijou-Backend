package com.smallnine.apiserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private String paymentMethod; // CREDIT_CARD, PAYPAL, BANK_TRANSFER, CASH
    private String status; // PENDING, COMPLETED, FAILED, REFUNDED
    private String transactionId;
    private String gatewayResponse;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime refundedAt;
}