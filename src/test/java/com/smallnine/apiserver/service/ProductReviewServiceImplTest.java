package com.smallnine.apiserver.service;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.OrderItemDao;
import com.smallnine.apiserver.dao.ProductReviewDao;
import com.smallnine.apiserver.entity.Review;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.impl.ProductReviewServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 純 Mockito unit test：聚焦 ProductReviewServiceImpl.add 的「需購買 + 去重」防護三條分支
 *  (a) 未購買          → 擋下 REVIEW_NOT_PURCHASED，不 insert
 *  (b) 已購買但已評價   → 擋下 REVIEW_ALREADY_EXISTS，不 insert
 *  (c) 已購買且未評價   → 正常 insert
 *
 * 不走 Spring context，避免 @Transactional / DB 真實連線。
 */
@ExtendWith(MockitoExtension.class)
class ProductReviewServiceImplTest {

    @Mock private ProductReviewDao productReviewDao;
    @Mock private OrderItemDao orderItemDao;

    @InjectMocks private ProductReviewServiceImpl productReviewService;

    private Review newReview() {
        Review r = new Review();
        r.setMemberId(1L);
        r.setProductId(100L);
        r.setRating(5);
        r.setContent("好物");
        return r;
    }

    @Test
    void add_whenNotPurchased_throwsNotPurchasedAndSkipsInsert() {
        Review review = newReview();
        when(orderItemDao.existsByMemberIdAndProductId(1L, 100L)).thenReturn(false);

        assertThatThrownBy(() -> productReviewService.add(review))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResponseCode.REVIEW_NOT_PURCHASED.getCode());

        verify(productReviewDao, never()).insert(any());
    }

    @Test
    void add_whenPurchasedButAlreadyReviewed_throwsAlreadyExistsAndSkipsInsert() {
        Review review = newReview();
        when(orderItemDao.existsByMemberIdAndProductId(1L, 100L)).thenReturn(true);
        when(productReviewDao.existsByMemberIdAndProductId(1L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> productReviewService.add(review))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResponseCode.REVIEW_ALREADY_EXISTS.getCode());

        verify(productReviewDao, never()).insert(any());
    }

    @Test
    void add_whenPurchasedAndNotReviewed_insertsNormally() {
        Review review = newReview();
        when(orderItemDao.existsByMemberIdAndProductId(1L, 100L)).thenReturn(true);
        when(productReviewDao.existsByMemberIdAndProductId(1L, 100L)).thenReturn(false);

        Review result = productReviewService.add(review);

        assertThat(result).isSameAs(review);
        verify(productReviewDao).insert(review);
    }
}
