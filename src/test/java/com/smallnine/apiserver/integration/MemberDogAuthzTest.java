package com.smallnine.apiserver.integration;

import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 批次4：Member(updateProfile/changePassword) + MemberDog(addDog/deleteDog) 授權紀律。
 *
 * 同批次2/3：現行 SecurityConfig `.anyRequest().authenticated()` 下匿名本就 401，
 * 補 @PreAuthorize("isAuthenticated()") 是顯式化 + defense-in-depth，測試為契約守門。
 * ownership 經查證 IDOR-safe（MemberServiceImpl.changePassword 比對
 * authenticatedUserId.equals(memberId)、DogServiceImpl.deleteDog 比對
 * existing.getMemberId().equals(memberId) → FORBIDDEN）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class MemberDogAuthzTest extends AbstractIntegrationTest {

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

    // ── 契約：匿名打全部 4 個寫入端點一律 401 ──

    @Test
    void memberWritesAnonymous_unauthorized() throws Exception {
        mockMvc.perform(put("/api/member/profile/edit"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/member/profile/1/password")
                        .param("currentPassword", "x").param("newPassword", "y"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dogWritesAnonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/member/dogs/add"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/member/dogs/1"))
                .andExpect(status().isUnauthorized());
    }

    // ── 沒被過度限制：已認證 USER 通過授權層落到下游（非 401/403，證明是 isAuthenticated 非 ADMIN）──

    @Test
    void changePasswordOwnId_asUser_passesAuthzThenBadPassword400() throws Exception {
        // path memberId=1 == 認證者 id=1 → ownership 通過 → 錯誤舊密碼 → 400（非 401/403）
        mockMvc.perform(put("/api/member/profile/1/password").with(user(seedUser()))
                        .param("currentPassword", "__definitely_wrong__")
                        .param("newPassword", "whatever123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteDog_asUser_passesAuthzThenNotFound404() throws Exception {
        mockMvc.perform(delete("/api/member/dogs/999999").with(user(seedUser())))
                .andExpect(status().isNotFound());
    }
}
