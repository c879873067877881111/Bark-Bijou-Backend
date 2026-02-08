package com.smallnine.apiserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SitterBookingRequest {

    @NotBlank(message = "開始日期不能為空")
    private String startDate;

    @NotBlank(message = "結束日期不能為空")
    private String endDate;

    @NotNull(message = "寵物ID不能為空")
    private Long petId;
}
