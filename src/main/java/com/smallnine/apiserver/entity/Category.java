package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String imageUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
}