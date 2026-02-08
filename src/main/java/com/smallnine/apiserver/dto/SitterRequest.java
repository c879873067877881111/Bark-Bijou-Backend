package com.smallnine.apiserver.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SitterRequest {

    @NotBlank(message = "姓名不能為空")
    private String name;

    @NotBlank(message = "服務地區不能為空")
    private String area;

    private String serviceTime;
    private String experience;
    private String introduction;

    @NotNull(message = "價格不能為空")
    @DecimalMin(value = "0", message = "價格不能為負數")
    private BigDecimal price;

    private String avatarUrl;
}
