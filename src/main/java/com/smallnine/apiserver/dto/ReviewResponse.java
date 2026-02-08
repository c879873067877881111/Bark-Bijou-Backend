package com.smallnine.apiserver.dto;

import com.smallnine.apiserver.entity.Review;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private Long id;
    private Long memberId;
    private Long productId;
    private Integer rating;
    private String content;
    private Boolean isVerifiedPurchase;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReviewResponse(Review review) {
        this.id = review.getId();
        this.memberId = review.getMemberId();
        this.productId = review.getProductId();
        this.rating = review.getRating();
        this.content = review.getContent();
        this.isVerifiedPurchase = review.getIsVerifiedPurchase();
        this.createdAt = review.getCreatedAt();
        this.updatedAt = review.getUpdatedAt();
    }

    public ReviewResponse(Review review, String username) {
        this(review);
        this.username = username;
    }
}
