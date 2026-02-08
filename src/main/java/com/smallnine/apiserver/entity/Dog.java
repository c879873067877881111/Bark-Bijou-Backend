package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dog {

    private Long id;
    private Long memberId;
    private String name;
    private String breed;
    private Integer age;
    private BigDecimal weight;
    private String gender;
    private String description;
    private String imageUrl;
    private String medicalNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
