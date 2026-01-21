package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {
    
    private Long id;
    
    private Long memberId;
    
    private Long productId;
    
    private LocalDateTime createdAt;
    
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}