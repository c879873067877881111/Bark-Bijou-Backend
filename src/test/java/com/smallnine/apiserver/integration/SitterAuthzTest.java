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
 * 批次6：SitterBooking(2) + Sitter(4) 授權紀律。
 *
 * 同批次2-5：補 @PreAuthorize("isAuthenticated()") 為顯式化 + defense-in-depth，
 * 測試為契約守門。ownership 經查證 IDOR-safe：
 * - SitterServiceImpl.updateSitter/deleteSitter 比對 sitter.getMemberId().equals(memberId) → SITTER_FORBIDDEN
 * - addDog/createSitter/createBooking 以登入者自身 id 為準（intrinsic）
 * - addReview 為任何登入者對某保母評論（非 owner，isAuthenticated 即正解）
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class SitterAuthzTest extends AbstractIntegrationTest {

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

    // ── 契約：匿名打全部 6 個寫入端點一律 401 ──

    @Test
    void sitterBookingWritesAnonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/sitter-booking/dogs")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/sitter-booking/1/bookings")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sitterWritesAnonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/sitter")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/sitter/1")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/sitter/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/sitter/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ── 沒被過度限制：已認證 USER 通過授權層落到精確下游碼（證明 isAuthenticated 非 ADMIN）──

    @Test
    void addDog_asUser_passesAuthzThenValidation400() throws Exception {
        // 授權通過 → @Valid（DogRequest 缺 name）→ 400，非 401/403
        mockMvc.perform(post("/api/sitter-booking/dogs").with(user(seedUser()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteSitter_asUser_passesAuthzThenNotFound404() throws Exception {
        // 授權通過 → 保母不存在 → 404，非 401/403
        mockMvc.perform(delete("/api/sitter/999999").with(user(seedUser())))
                .andExpect(status().isNotFound());
    }
}
