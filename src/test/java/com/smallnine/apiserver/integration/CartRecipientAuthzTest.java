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
 * #M1 批次3：Cart(4) + Recipient(3) 寫入端點授權紀律。
 *
 * 與批次2 同：現行 SecurityConfig `.anyRequest().authenticated()` 下匿名本就 401，
 * 補 @PreAuthorize("isAuthenticated()") 是顯式化 + defense-in-depth，測試為契約守門。
 * ownership 已由 service 層把關（CartServiceImpl / RecipientServiceImpl 皆檢查
 * getMemberId().equals(memberId) → FORBIDDEN，IDOR-safe），屬 #M1 範圍非 #M2。
 * 跨租戶 ownership 行為由各 service 既有測試覆蓋，本檔只驗 authz 註解契約。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CartRecipientAuthzTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void cartWritesAnonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/cart/items/1").param("quantity", "1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/cart/items/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recipientWritesAnonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/me/recipients")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/auth/me/recipients/1")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/auth/me/recipients/1"))
                .andExpect(status().isUnauthorized());
    }

    // ── 沒被過度限制：已認證 USER 通過授權層，落到下游（精確碼，非 401/403）──

    @Test
    void addToCart_asUser_passesAuthzThenValidation400() throws Exception {
        mockMvc.perform(post("/api/cart/items").with(user(seedUser()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clearCart_asUser_passesAuthzThenOk() throws Exception {
        mockMvc.perform(delete("/api/cart").with(user(seedUser())))
                .andExpect(status().isOk());
    }

    @Test
    void addRecipient_asUser_passesAuthzThenValidation400() throws Exception {
        mockMvc.perform(post("/api/auth/me/recipients").with(user(seedUser()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteRecipient_asUser_passesAuthzThenNotFound404() throws Exception {
        mockMvc.perform(delete("/api/auth/me/recipients/999999").with(user(seedUser())))
                .andExpect(status().isNotFound());
    }

    // PUT 端點正向斷言（合法輸入 + 不存在 id → 授權通過後落到 404，非 401/403）

    @Test
    void updateCartItem_asUser_passesAuthzThenNotFound404() throws Exception {
        mockMvc.perform(put("/api/cart/items/999999").param("quantity", "1")
                        .with(user(seedUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRecipient_asUser_passesAuthzThenNotFound404() throws Exception {
        mockMvc.perform(put("/api/auth/me/recipients/999999").with(user(seedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"phone\":\"0900000000\"}"))
                .andExpect(status().isNotFound());
    }
}
