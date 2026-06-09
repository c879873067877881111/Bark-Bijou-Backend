package com.smallnine.apiserver.security;

import com.smallnine.apiserver.entity.Article;
import com.smallnine.apiserver.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * 方法級授權判斷集中地，給 {@code @PreAuthorize} SpEL 以 {@code @securityService.xxx(...)} 呼叫。
 * 把原本內嵌在 SpEL 的 DB 查詢搬進可測試的 Java，順帶修掉舊式 {@code authentication.principal.id}
 * （UserPrincipal 並無 id 屬性，非 ADMIN 求值即丟例外）。
 */
@Component
@RequiredArgsConstructor
public class SecurityService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ArticleService articleService;

    /**
     * 文章更新／刪除的擁有權檢查：ADMIN 或文章作者本人可操作。
     * 文章不存在時沿用原行為，由 {@link ArticleService#findById} 丟出 404。
     */
    public boolean canEditArticle(Authentication authentication, Long articleId) {
        Long userId = currentUserId(authentication);
        if (userId == null) {
            return false;
        }
        if (hasRole(authentication, ROLE_ADMIN)) {
            return true;
        }
        Article article = articleService.findById(articleId);
        return userId.equals(article.getMemberId());
    }

    // 從認證主體取登入者 id；未登入或主體非 UserPrincipal 回傳 null
    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getUser().getId();
    }

    private boolean hasRole(Authentication authentication, String role) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}