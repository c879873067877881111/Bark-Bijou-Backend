package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Article {
    
    private Long id;
    
    private Long memberId;
    
    private String memberUsername;
    
    private String author;
    
    private String title;
    
    private Long dogsId;
    
    private String dogsBreed;
    
    private String dogsImages;
    
    private String content1;
    
    private String content2;
    
    private LocalDateTime createdDate;
    
    private Long createdId;
    
    private Long eventId;
    
    private Integer valid;
    
    private String articleImages;
    
    private String categoryName;
    
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
}