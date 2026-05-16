package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.dto.FavoriteRequest;
import com.smallnine.apiserver.entity.Favorite;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.ProductFavoriteService;
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
@RequestMapping("/api/product/favorite")
@RequiredArgsConstructor
@Tag(name = "商品收藏", description = "商品收藏管理 API")
public class ProductFavoriteController {

    private final ProductFavoriteService productFavoriteService;

    @Operation(summary = "取得我的收藏列表")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Favorite>>> getMyFavorites(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        List<Favorite> favorites = productFavoriteService.getByMemberId(user.getId());
        return ResponseEntity.ok(ApiResponse.success(favorites));
    }

    @Operation(summary = "切換收藏狀態")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FavoriteRequest request) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Long productId = request.getProductId();

        boolean isFavorite = productFavoriteService.isFavorite(user.getId(), productId);
        if (isFavorite) {
            productFavoriteService.remove(user.getId(), productId);
            return ResponseEntity.ok(ApiResponse.success(Map.of("favorite", false)));
        } else {
            productFavoriteService.add(user.getId(), productId);
            return ResponseEntity.ok(ApiResponse.success(Map.of("favorite", true)));
        }
    }
}
