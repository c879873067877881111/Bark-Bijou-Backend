package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {
    
    private Long id;
    
    private Long productId;
    
    private String imageUrl;
    
    private String altText;
    
    private Boolean isPrimary = false;
    
    private Integer sortOrder = 0;
    
    private LocalDateTime createdAt;
    
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}