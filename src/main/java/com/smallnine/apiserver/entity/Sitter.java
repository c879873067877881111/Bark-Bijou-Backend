package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sitter {

    private Long id;
    private Long memberId;
    private String name;
    private String area;
    private String serviceTime;
    private String experience;
    private String introduction;
    private BigDecimal price;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
