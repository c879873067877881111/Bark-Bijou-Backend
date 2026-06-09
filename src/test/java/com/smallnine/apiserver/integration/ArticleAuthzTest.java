package com.smallnine.apiserver.integration;

import com.smallnine.apiserver.entity.Article;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.security.UserPrincipal;
import com.smallnine.apiserver.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #M2：文章更新／刪除授權改走 @securityService.canEditArticle（取代 SpEL 內嵌 DB 查詢）。
 * 真實 wiring 驗證 SpEL bean 接得上，並回歸保證：修掉舊 authentication.principal.id 後，
 * 文章作者本人現在真的能改自己的文章（舊寫法對非 ADMIN 求值即丟例外）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ArticleAuthzTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ArticleService articleService;

    // 用真實 UserPrincipal 當 principal（Spring 內建 user() 產的不是 UserPrincipal，canEditArticle 會判 false）
    private RequestPostProcessor asUser(long id, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        u.setRole(role);
        UserPrincipal principal = new UserPrincipal(u);
        return authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Long createArticleOwnedBy(long memberId) {
        Article a = new Article();
        a.setMemberId(memberId);
        a.setMemberUsername("user" + memberId);
        a.setAuthor("user" + memberId);
        a.setTitle("t");
        a.setContent1("c");
        a.setCreatedId(memberId);
        a.setCategoryName("general");
        return articleService.createArticle(a).getId();
    }

    // 更新走全欄位覆寫，NOT NULL 欄位都要帶（缺了會在 DB 層 409，與授權無關）
    private static final String BODY = "{\"memberUsername\":\"user1\",\"author\":\"user1\","
            + "\"title\":\"updated\",\"content1\":\"c\",\"valid\":1,\"categoryName\":\"general\"}";

    @Test
    void update_anonymous_unauthorized() throws Exception {
        Long id = createArticleOwnedBy(1L);

        mockMvc.perform(put("/api/articles/" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_nonOwner_forbidden() throws Exception {
        Long id = createArticleOwnedBy(1L);

        mockMvc.perform(put("/api/articles/" + id).with(asUser(2L, User.Role.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_nonOwner_forbidden() throws Exception {
        Long id = createArticleOwnedBy(1L);

        mockMvc.perform(delete("/api/articles/" + id).with(asUser(2L, User.Role.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_owner_ok() throws Exception {
        Long id = createArticleOwnedBy(1L);

        mockMvc.perform(put("/api/articles/" + id).with(asUser(1L, User.Role.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    void delete_admin_ok() throws Exception {
        Long id = createArticleOwnedBy(1L);

        mockMvc.perform(delete("/api/articles/" + id).with(asUser(9999L, User.Role.ADMIN)))
                .andExpect(status().isOk());
    }
}