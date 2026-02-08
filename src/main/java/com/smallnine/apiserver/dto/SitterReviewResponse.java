package com.smallnine.apiserver.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SitterReviewResponse {

    private Integer rating;
    private String comment;
    private String username;
    private String imageUrl;
    private LocalDateTime createdAt;
}
