package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.ProductReviewDao;
import com.smallnine.apiserver.entity.Review;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductReviewDao productReviewDao;

    @Override
    public List<Review> getByProductId(Long productId) {
        return productReviewDao.findByProductId(productId);
    }

    @Override
    @Transactional
    public Review add(Review review) {
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
