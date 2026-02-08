package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.entity.Favorite;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.ProductFavoriteService;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String, Object>> getMyFavorites(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("data", List.of()));
        }
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        List<Favorite> favorites = productFavoriteService.getByMemberId(user.getId());
        return ResponseEntity.ok(Map.of("data", favorites));
    }

    @Operation(summary = "切換收藏狀態")
    @PostMapping
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Long productId = Long.valueOf(body.get("productId").toString());

        boolean isFavorite = productFavoriteService.isFavorite(user.getId(), productId);
        if (isFavorite) {
            productFavoriteService.remove(user.getId(), productId);
            return ResponseEntity.ok(Map.of("favorite", false));
        } else {
            productFavoriteService.add(user.getId(), productId);
            return ResponseEntity.ok(Map.of("favorite", true));
        }
    }
}
