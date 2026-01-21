package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Brand {
    
    private Long id;
    private String name;
    private String description;
    private String logoUrl;
    private LocalDateTime createdAt;
}