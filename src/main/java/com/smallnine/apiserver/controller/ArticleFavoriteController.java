package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.ArticleFavoriteService;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles/favorites")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "文章收藏", description = "文章收藏管理 API")
public class ArticleFavoriteController {

    private final ArticleFavoriteService articleFavoriteService;

    @Operation(summary = "取得熱門收藏文章（公開）")
    @GetMapping("/top")
    public ApiResponse<List<Map<String, Object>>> getTopFavoritedArticles(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(articleFavoriteService.getTopFavoritedArticles(limit));
    }

    @Operation(summary = "取得我的收藏文章 ID 列表")
    @GetMapping
    public ApiResponse<List<Long>> getMyFavorites(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        return ApiResponse.success(articleFavoriteService.getFavoriteArticleIds(user.getId()));
    }

    @Operation(summary = "收藏文章")
    @PostMapping("/{articleId}")
    public ApiResponse<Map<String, Object>> addFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long articleId) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        articleFavoriteService.addFavorite(user.getId(), articleId);
        int count = articleFavoriteService.countByArticleId(articleId);
        return ApiResponse.success(Map.of("favorite", true, "count", count));
    }

    @Operation(summary = "取消收藏文章")
    @DeleteMapping("/{articleId}")
    public ApiResponse<Map<String, Object>> removeFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long articleId) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        articleFavoriteService.removeFavorite(user.getId(), articleId);
        int count = articleFavoriteService.countByArticleId(articleId);
        return ApiResponse.success(Map.of("favorite", false, "count", count));
    }
}
