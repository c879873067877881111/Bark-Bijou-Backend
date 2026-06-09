package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.OrderItemDao;
import com.smallnine.apiserver.dao.ProductReviewDao;
import com.smallnine.apiserver.entity.Review;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductReviewDao productReviewDao;
    private final OrderItemDao orderItemDao;

    @Override
    public List<Review> getByProductId(Long productId) {
        return productReviewDao.findByProductId(productId);
    }

    @Override
    @Transactional
    public Review add(Review review) {
        // 需購買：該會員對此商品須有有效購買紀錄才可評價
        if (!orderItemDao.existsByMemberIdAndProductId(review.getMemberId(), review.getProductId())) {
            throw new BusinessException(ResponseCode.REVIEW_NOT_PURCHASED);
        }
        // 去重：同一會員對同一商品只能評價一次
        if (productReviewDao.existsByMemberIdAndProductId(review.getMemberId(), review.getProductId())) {
            throw new BusinessException(ResponseCode.REVIEW_ALREADY_EXISTS);
        }
        productReviewDao.insert(review);
        return review;
    }

    @Override
    @Transactional
    public Review update(Long id, Review review, Long memberId) {
        Review existing = productReviewDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.REVIEW_NOT_FOUND));
        if (!existing.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }
        review.setId(id);
        review.setMemberId(memberId);
        review.setProductId(existing.getProductId());
        productReviewDao.update(review);
        return productReviewDao.findById(id).orElse(review);
    }

    @Override
    @Transactional
    public void delete(Long id, Long memberId) {
        Review existing = productReviewDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.REVIEW_NOT_FOUND));
        if (!existing.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }
        productReviewDao.deleteById(id);
    }
}
