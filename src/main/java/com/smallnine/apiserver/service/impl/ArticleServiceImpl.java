package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.ArticleDao;
import com.smallnine.apiserver.entity.Article;
import com.smallnine.apiserver.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl {

    private final ArticleDao articleDao;

    public Article findById(Long id) {
        return articleDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.ARTICLE_NOT_FOUND));
    }

    public List<Article> findAll(int page, int size) {
        int offset = page * size;
        return articleDao.findAll(offset, size);
    }

    public List<Article> findByAuthor(String author) {
        return articleDao.findByAuthor(author);
    }

    public List<Article> findByCategoryName(String categoryName) {
        return articleDao.findByCategoryName(categoryName);
    }

    public List<Article> searchByTitle(String title, int page, int size) {
        int offset = page * size;
        return articleDao.searchByTitle(title, offset, size);
    }

    public List<Article> findByMemberId(Long memberId) {
        return articleDao.findByMemberId(memberId);
    }

    @Transactional
    public Article createArticle(Article article) {
        if (article.getCreatedDate() == null) {
            article.setCreatedDate(LocalDateTime.now());
        }
        if (article.getValid() == null) {
            article.setValid(1);
        }

        int result = articleDao.insert(article);
        if (result == 0) {
            throw new BusinessException(ResponseCode.ARTICLE_CREATE_FAILED);
        }

        log.info("action=CREATE_ARTICLE id={} title={}", article.getId(), article.getTitle());
        return article;
    }

    @Transactional
    public Article updateArticle(Article article) {
        findById(article.getId());

        int result = articleDao.update(article);
        if (result == 0) {
            throw new BusinessException(ResponseCode.ARTICLE_UPDATE_FAILED);
        }

        log.info("action=UPDATE_ARTICLE id={} title={}", article.getId(), article.getTitle());
        return findById(article.getId());
    }

    @Transactional
    public void deleteArticle(Long id) {
        Article article = findById(id);

        int result = articleDao.deleteById(id);
        if (result == 0) {
            throw new BusinessException(ResponseCode.ARTICLE_DELETE_FAILED);
        }

        log.info("action=DELETE_ARTICLE id={} title={}", id, article.getTitle());
    }

    public long count() {
        return articleDao.count();
    }

    public long countValid() {
        return articleDao.countValid();
    }
}
