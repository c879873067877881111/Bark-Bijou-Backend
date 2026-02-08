package com.smallnine.apiserver.service;

import java.util.List;
import java.util.Map;

public interface ArticleFavoriteService {

    List<Long> getFavoriteArticleIds(Long memberId);

    List<Map<String, Object>> getTopFavoritedArticles(int limit);

    int addFavorite(Long memberId, Long articleId);

    int removeFavorite(Long memberId, Long articleId);

    int countByArticleId(Long articleId);

    boolean isFavorite(Long memberId, Long articleId);
}
