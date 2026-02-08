package com.smallnine.apiserver.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcpayCreateRequest {
    @NotNull
    private Long orderId;
    @NotNull
    private BigDecimal totalAmount;
    private String itemName;
}
