package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.dao.ArticleDao;
import com.smallnine.apiserver.dao.ArticleFavoriteDao;
import com.smallnine.apiserver.entity.Article;
import com.smallnine.apiserver.service.ArticleFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ArticleFavoriteServiceImpl implements ArticleFavoriteService {

    private final ArticleFavoriteDao articleFavoriteDao;
    private final ArticleDao articleDao;

    @Override
    public List<Long> getFavoriteArticleIds(Long memberId) {
        return articleFavoriteDao.findArticleIdsByMemberId(memberId);
    }

    @Override
    @Transactional
    public int addFavorite(Long memberId, Long articleId) {
        if (articleFavoriteDao.existsByMemberIdAndArticleId(memberId, articleId)) {
            return 0;
        }
        return articleFavoriteDao.insert(memberId, articleId);
    }

    @Override
    @Transactional
    public int removeFavorite(Long memberId, Long articleId) {
        return articleFavoriteDao.deleteByMemberIdAndArticleId(memberId, articleId);
    }

    @Override
    public int countByArticleId(Long articleId) {
        return articleFavoriteDao.countByArticleId(articleId);
    }

    @Override
    public boolean isFavorite(Long memberId, Long articleId) {
        return articleFavoriteDao.existsByMemberIdAndArticleId(memberId, articleId);
    }

    @Override
    public List<Map<String, Object>> getTopFavoritedArticles(int limit) {
        List<Map<String, Object>> topIds = articleFavoriteDao.findTopFavoritedArticleIds(limit);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> entry : topIds) {
            Long articleId = ((Number) entry.get("article_id")).longValue();
            int favoriteCount = ((Number) entry.get("favorite_count")).intValue();
            articleDao.findById(articleId).ifPresent(article -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", article.getId());
                item.put("title", article.getTitle());
                item.put("author", article.getAuthor());
                item.put("articleImages", article.getArticleImages());
                item.put("categoryName", article.getCategoryName());
                item.put("content1", article.getContent1());
                item.put("createdDate", article.getCreatedDate());
                item.put("favoriteCount", favoriteCount);
                result.add(item);
            });
        }
        return result;
    }
}
