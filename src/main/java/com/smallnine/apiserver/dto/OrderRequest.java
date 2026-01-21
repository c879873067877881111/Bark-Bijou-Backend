package com.smallnine.apiserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderRequest {
    
    @NotBlank(message = "配送地址不能為空")
    @Size(max = 500, message = "配送地址長度不能超過500字符")
    private String shippingAddress;
    
    @Size(max = 1000, message = "備註長度不能超過1000字符")
    private String notes;
    
    @NotBlank(message = "支付方式不能為空")
    private String paymentMethod;
}