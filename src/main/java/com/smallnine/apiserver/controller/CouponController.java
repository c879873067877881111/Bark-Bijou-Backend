package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.dto.ClaimCouponRequest;
import com.smallnine.apiserver.dto.CouponResponse;
import com.smallnine.apiserver.entity.Coupon;
import com.smallnine.apiserver.entity.MemberCoupon;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.CouponService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
@Tag(name = "優惠券", description = "優惠券管理 API")
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "取得所有可用優惠券")
    @GetMapping("/coupons")
    public ResponseEntity<ApiResponse<List<Coupon>>> getAllCoupons() {
        List<Coupon> coupons = couponService.getAllActive();
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    @Operation(summary = "取得單一優惠券 (含 isClaimed)")
    @GetMapping("/coupons/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> getCoupon(
            @PathVariable Long id,
            @RequestParam(required = false) Long memberId) {
        CouponResponse coupon = couponService.getById(id, memberId);
        return ResponseEntity.ok(ApiResponse.success(coupon));
    }

    @Operation(summary = "領取優惠券")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/claim")
    public ResponseEntity<ApiResponse<MemberCoupon>> claimCoupon(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ClaimCouponRequest request) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        MemberCoupon mc = couponService.claim(request.getCouponId(), user.getId());
        return ResponseEntity.ok(ApiResponse.success("領取成功", mc));
    }

    @Operation(summary = "取得會員優惠券 (依類型)")
    @GetMapping("/members/me/coupons/{type}")
    public ResponseEntity<ApiResponse<List<MemberCoupon>>> getMemberCouponsByType(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String type) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        List<MemberCoupon> coupons = couponService.getMemberCoupons(user.getId());
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    @Operation(summary = "取得會員已領取的優惠券")
    @GetMapping("/member/me")
    public ResponseEntity<ApiResponse<List<MemberCoupon>>> getMemberCoupons(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success(couponService.getMemberCoupons(user.getId())));
    }
}
