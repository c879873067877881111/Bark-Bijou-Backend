package com.smallnine.apiserver.security;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.entity.Article;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 把 ArticleController @PreAuthorize 內嵌的 DB 查詢抽到 SecurityService。
 * 涵蓋 ADMIN、作者本人、非作者、未登入、主體非 UserPrincipal、文章不存在六種情形。
 */
@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private ArticleService articleService;

    @InjectMocks
    private SecurityService securityService;

    // 用真實 UserPrincipal 包 User，權限沿用 UserPrincipal.getAuthorities()（ROLE_<role>）
    private Authentication authFor(Long userId, User.Role role) {
        User user = new User();
        user.setId(userId);
        user.setRole(role);
        UserPrincipal principal = new UserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void admin_canEdit_withoutLoadingArticle() {
        Authentication auth = authFor(99L, User.Role.ADMIN);

        boolean result = securityService.canEditArticle(auth, 1L);

        assertThat(result).isTrue();
        // ADMIN 直接放行，不該白白查一次 DB
        verify(articleService, never()).findById(any());
    }

    @Test
    void owner_canEdit() {
        Authentication auth = authFor(7L, User.Role.USER);
        Article article = new Article();
        article.setMemberId(7L);
        when(articleService.findById(1L)).thenReturn(article);

        assertThat(securityService.canEditArticle(auth, 1L)).isTrue();
    }

    @Test
    void nonOwner_cannotEdit() {
        Authentication auth = authFor(7L, User.Role.USER);
        Article article = new Article();
        article.setMemberId(8L);
        when(articleService.findById(1L)).thenReturn(article);

        assertThat(securityService.canEditArticle(auth, 1L)).isFalse();
    }

    @Test
    void nullAuthentication_cannotEdit() {
        assertThat(securityService.canEditArticle(null, 1L)).isFalse();
        verify(articleService, never()).findById(any());
    }

    @Test
    void nonUserPrincipal_cannotEdit() {
        Authentication auth = new UsernamePasswordAuthenticationToken("anonymous", null);

        assertThat(securityService.canEditArticle(auth, 1L)).isFalse();
        verify(articleService, never()).findById(any());
    }

    @Test
    void articleNotFound_throwsNotFound() {
        Authentication auth = authFor(7L, User.Role.USER);
        when(articleService.findById(1L))
                .thenThrow(new BusinessException(ResponseCode.ARTICLE_NOT_FOUND));

        assertThatThrownBy(() -> securityService.canEditArticle(auth, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ResponseCode.ARTICLE_NOT_FOUND.getCode());
    }
}