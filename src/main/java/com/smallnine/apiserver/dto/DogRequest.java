package com.smallnine.apiserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DogRequest {
    @NotBlank(message = "寵物名稱不能為空")
    private String name;
    private String breed;
    private Integer age;
    private BigDecimal weight;
    private String gender;
    private String description;
    private String imageUrl;
    private String medicalNotes;
}
