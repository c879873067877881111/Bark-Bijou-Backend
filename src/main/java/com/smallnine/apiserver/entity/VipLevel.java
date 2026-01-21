package com.smallnine.apiserver.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VipLevel {

    private Long id;

    private String name;

    private BigDecimal minSpending;

    private BigDecimal discountRate;

    private BigDecimal pointsMultiplier;

    private BigDecimal freeShippingThreshold;

    private String benefits;

    private Boolean isActive = true;

    private Integer sortOrder;
}