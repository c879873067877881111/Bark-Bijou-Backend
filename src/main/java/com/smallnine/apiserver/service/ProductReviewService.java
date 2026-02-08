package com.smallnine.apiserver.service;

import com.smallnine.apiserver.entity.Review;

import java.util.List;

public interface ProductReviewService {

    List<Review> getByProductId(Long productId);

    Review add(Review review);

    Review update(Long id, Review review, Long memberId);

    void delete(Long id, Long memberId);
}
