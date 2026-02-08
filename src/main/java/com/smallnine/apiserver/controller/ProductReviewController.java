package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.entity.Review;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.ProductReviewService;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @Operation(summary = "檢查是否可評價")
    @GetMapping("/{productId}/check")
    public ResponseEntity<Map<String, Object>> checkReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hasPurchased", true); // Default to true for now
        data.put("hasCommented", false);
        data.put("review", null);

        if (userDetails != null) {
            User user = AuthUtils.getAuthenticatedUser(userDetails);
            // Check if user has already reviewed this product
            var reviews = productReviewService.getByProductId(productId);
            var existing = reviews.stream()
                    .filter(r -> r.getMemberId().equals(user.getId()))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                data.put("hasCommented", true);
                data.put("review", existing);
            }
        }

        return ResponseEntity.ok(Map.of("data", data));
    }

    @Operation(summary = "新增評價")
    @PostMapping
    public ResponseEntity<Map<String, Object>> addReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);

        Long productId = Long.valueOf(body.get("productId").toString());
        Integer rating = Integer.valueOf(body.get("rating").toString());
        String comment = body.get("comment") != null ? body.get("comment").toString() : "";

        Review review = new Review();
        review.setProductId(productId);
        review.setMemberId(user.getId());
        review.setRating(rating);
        review.setContent(comment);
        review.setIsVerifiedPurchase(false);

        Review created = productReviewService.add(review);
        return ResponseEntity.ok(Map.of("message", "評價成功", "review", created));
    }
}
