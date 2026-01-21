package com.smallnine.apiserver.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleResponse {
    
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
}