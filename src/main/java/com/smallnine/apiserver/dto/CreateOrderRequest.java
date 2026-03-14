package com.smallnine.apiserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "收件人姓名不能為空")
    @Size(max = 100, message = "收件人姓名長度不能超過100字符")
    private String recipientName;

    @NotBlank(message = "收件人電話不能為空")
    @Size(max = 20, message = "收件人電話長度不能超過20字符")
    private String recipientPhone;

    @Size(max = 255, message = "收件人Email長度不能超過255字符")
    private String recipientEmail;

    @NotBlank(message = "配送方式不能為空")
    @Size(max = 20, message = "配送方式長度不能超過20字符")
    private String deliveryMethod;

    @Size(max = 50, message = "城市長度不能超過50字符")
    private String city;

    @Size(max = 50, message = "鄉鎮區長度不能超過50字符")
    private String town;

    @Size(max = 500, message = "地址長度不能超過500字符")
    private String address;

    @Size(max = 100, message = "門市名稱長度不能超過100字符")
    private String storeName;

    @Size(max = 500, message = "門市地址長度不能超過500字符")
    private String storeAddress;

    @Size(max = 500, message = "配送地址長度不能超過500字符")
    private String shippingAddress;

    @Size(max = 1000, message = "備註長度不能超過1000字符")
    private String notes;

    private Integer couponId;
}
