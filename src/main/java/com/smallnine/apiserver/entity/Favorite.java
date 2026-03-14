package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品收藏實體，對應 product_favorites 表。
 * 文章收藏由 ArticleFavoriteDao 直接以 @Param 處理，不使用此實體。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {

    private Long id;

    private Long memberId;

    private Long productId;

    private LocalDateTime createdAt;
}
