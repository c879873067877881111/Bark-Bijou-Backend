package com.smallnine.apiserver.integration;

import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #M1 批次5：Notification(4) + ProductFavorite(1) + ArticleFavorite(2) 授權紀律。
 *
 * 同批次2-4：補 @PreAuthorize("isAuthenticated()") 為顯式化 + defense-in-depth，
 * 測試為契約守門。ownership 經查證 IDOR-safe：
 * - NotificationServiceImpl.markAsRead/delete 比對 n.getMemberId().equals(memberId) → FORBIDDEN
 * - favorites 為 (memberId, targetId) 複合自限，操作對象恆為登入者自身
 * 故屬 #M1 非 #M2。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class NotificationFavoriteAuthzTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private UserPrincipal seedUser() {
        User u = new User();
        u.setId(1L);
        u.setUsername("authz-user");
        u.setRole(User.Role.USER);
        u.setEmailValidated(true);
        return new UserPrincipal(u);
    }

    // ── 契約：匿名打全部 7 個寫入端點一律 401 ──

    @Test
    void notificationWritesAnonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/notifications/read/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/notifications/read-all"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/notifications/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void favoriteWritesAnonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/product/favorite")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/articles/favorites/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/articles/favorites/1"))
                .andExpect(status().isUnauthorized());
    }

    // ── 沒被過度限制：已認證 USER 通過授權層落到精確下游碼（證明 isAuthenticated 非 ADMIN）──

    @Test
    void deleteNotification_asUser_passesAuthzThenNotFound404() throws Exception {
        mockMvc.perform(delete("/api/notifications/999999").with(user(seedUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAllAsRead_asUser_passesAuthzThenOk() throws Exception {
        mockMvc.perform(post("/api/notifications/read-all").with(user(seedUser())))
                .andExpect(status().isOk());
    }

    @Test
    void toggleProductFavorite_asUser_passesAuthzThenValidation400() throws Exception {
        // 授權通過 → @Valid（缺 productId）→ 400，非 401/403
        mockMvc.perform(post("/api/product/favorite").with(user(seedUser()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeArticleFavorite_asUser_passesAuthzThenOk() throws Exception {
        // removeFavorite 冪等（刪 0 列也回 200），證明授權通過且非 ADMIN
        mockMvc.perform(delete("/api/articles/favorites/999999").with(user(seedUser())))
                .andExpect(status().isOk());
    }
}
