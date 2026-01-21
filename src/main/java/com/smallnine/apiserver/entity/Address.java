package com.smallnine.apiserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    
    private Long id;
    private Long userId;
    private String recipientName;
    private String phone;
    private String country;
    private String province;
    private String city;
    private String district;
    private String street;
    private String postalCode;
    private Boolean isDefault;
    private String label; // 家、公司等
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}