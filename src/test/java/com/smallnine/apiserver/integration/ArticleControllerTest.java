package com.smallnine.apiserver.integration;

import com.smallnine.apiserver.entity.Article;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.security.UserPrincipal;
import com.smallnine.apiserver.service.ArticleService;
import com.smallnine.apiserver.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ArticleController 讀取／建立路徑的覆蓋（寫入授權路徑已由 ArticleAuthzTest 覆蓋）。
 * createArticle 回傳 ApiResponse 物件而非 ResponseEntity，故 HTTP 仍是 200，body.code=201。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ArticleControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleService articleService;

    // 圖片分支會寫檔到磁碟，mock 掉避免真實 IO 副作用
    @MockitoBean
    private FileStorageService fileStorageService;

    private RequestPostProcessor asUser(long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        u.setRole(User.Role.USER);
        UserPrincipal principal = new UserPrincipal(u);
        return authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Long createSeedArticle() {
        Article a = new Article();
        a.setMemberId(1L);
        a.setMemberUsername("user1");
        a.setAuthor("user1");
        a.setTitle("t");
        a.setContent1("c");
        a.setCreatedId(1L);
        a.setCategoryName("general");
        return articleService.createArticle(a).getId();
    }

    // ── createArticle ──

    @Test
    void create_mapsPrincipalAndCategoryFallback() throws Exception {
        mockMvc.perform(multipart("/api/articles")
                        .param("title", "hello")
                        .param("content1", "body")
                        .param("category", "news")
                        .with(asUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.author").value("user1"))
                .andExpect(jsonPath("$.data.memberUsername").value("user1"))
                .andExpect(jsonPath("$.data.categoryName").value("news")); // fallback 生效
    }

    @Test
    void create_withImages_joinsUrls() throws Exception {
        when(fileStorageService.store(any(), eq("articles")))
                .thenReturn("/uploads/articles/a.jpg", "/uploads/articles/b.jpg");

        MockMultipartFile img1 = new MockMultipartFile("images", "a.jpg", "image/jpeg", new byte[]{1});
        MockMultipartFile img2 = new MockMultipartFile("images", "b.jpg", "image/jpeg", new byte[]{2});

        mockMvc.perform(multipart("/api/articles")
                        .file(img1).file(img2)
                        .param("title", "hello")
                        .param("content1", "body")
                        .param("category", "news")
                        .with(asUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articleImages")
                        .value("/uploads/articles/a.jpg,/uploads/articles/b.jpg"));
    }

    @Test
    void create_categoryNamePresent_fallbackSkipped() throws Exception {
        // category param 有給，但 categoryName 已有值 → 不該被 fallback 覆蓋
        mockMvc.perform(multipart("/api/articles")
                        .param("title", "hello")
                        .param("content1", "body")
                        .param("categoryName", "tech")
                        .param("category", "news")
                        .with(asUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryName").value("tech"));
    }

    @Test
    void create_noCategoryParam_keepsProvidedName() throws Exception {
        // 不帶 category param（category == null）→ 短路，沿用 body 的 categoryName
        mockMvc.perform(multipart("/api/articles")
                        .param("title", "hello")
                        .param("content1", "body")
                        .param("categoryName", "tech")
                        .with(asUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryName").value("tech"));
    }

    @Test
    void create_emptyImageSkipped() throws Exception {
        // 空檔該被 f.isEmpty() 跳過，只存非空那一張
        when(fileStorageService.store(any(), eq("articles")))
                .thenReturn("/uploads/articles/a.jpg");

        MockMultipartFile empty = new MockMultipartFile("images", "empty.jpg", "image/jpeg", new byte[0]);
        MockMultipartFile real = new MockMultipartFile("images", "a.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/articles")
                        .file(empty).file(real)
                        .param("title", "hello")
                        .param("content1", "body")
                        .param("category", "news")
                        .with(asUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articleImages").value("/uploads/articles/a.jpg"));
    }

    // ── 讀取路徑（委派，驗 200 與信封）──

    @Test
    void getArticleById_ok() throws Exception {
        Long id = createSeedArticle();

        mockMvc.perform(get("/api/articles/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void getArticles_returnsList() throws Exception {
        mockMvc.perform(get("/api/articles").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void searchArticles_ok() throws Exception {
        mockMvc.perform(get("/api/articles/search").param("title", "t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getArticlesByAuthor_ok() throws Exception {
        mockMvc.perform(get("/api/articles/author/user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getArticlesByCategory_ok() throws Exception {
        mockMvc.perform(get("/api/articles/category/general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getArticlesByMember_ok() throws Exception {
        mockMvc.perform(get("/api/articles/member/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getArticleStats_ok() throws Exception {
        mockMvc.perform(get("/api/articles/stats/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalArticles").isNumber())
                .andExpect(jsonPath("$.data.validArticles").isNumber());
    }
}