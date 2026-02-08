package com.smallnine.apiserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecipientRequest {
    @NotBlank(message = "收件人姓名不能為空")
    private String name;

    @NotBlank(message = "電話不能為空")
    private String phone;

    private String city;
    private String town;
    private String address;
    private Boolean isDefault;
}
