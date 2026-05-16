package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dao.OrderItemDao;
import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.dto.ReviewRequest;
import com.smallnine.apiserver.entity.Review;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.ProductReviewService;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/product/review")
@RequiredArgsConstructor
@Tag(name = "商品評價", description = "商品評價管理 API")
public class ProductReviewController {

    private final ProductReviewService productReviewService;
    private final OrderItemDao orderItemDao;

    @Operation(summary = "檢查是否可評價")
    @GetMapping("/{productId}/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hasPurchased", false);
        data.put("hasCommented", false);
        data.put("review", null);

        if (userDetails != null) {
            User user = AuthUtils.getAuthenticatedUser(userDetails);
            data.put("hasPurchased", orderItemDao.existsByMemberIdAndProductId(user.getId(), productId));

            var reviews = productReviewService.getByProductId(productId);
            Review existing = null;
            for (Review r : reviews) {
                if (r.getMemberId().equals(user.getId())) {
                    existing = r;
                    break;
                }
            }
            if (existing != null) {
                data.put("hasCommented", true);
                data.put("review", existing);
            }
        }

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "新增評價")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponse<Review>> addReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewRequest request) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);

        Review review = new Review();
        review.setProductId(request.getProductId());
        review.setMemberId(user.getId());
        review.setRating(request.getRating());
        review.setContent(request.getContent() != null ? request.getContent() : "");
        review.setIsVerifiedPurchase(orderItemDao.existsByMemberIdAndProductId(user.getId(), request.getProductId()));

        Review created = productReviewService.add(review);
        return ResponseEntity.ok(ApiResponse.success("評價成功", created));
    }
}
