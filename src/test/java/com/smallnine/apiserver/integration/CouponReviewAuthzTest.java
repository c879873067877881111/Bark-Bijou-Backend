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
 * #M1 批次7（最後一批）：Coupon(1) + ProductReview(1) 授權紀律。
 *
 * 同批次2-6：補 @PreAuthorize("isAuthenticated()") 為顯式化 + defense-in-depth，
 * 測試為契約守門。C 類純認證——兩個寫入端點皆以登入者自身 id 為準（intrinsic），
 * 無 path-id 反查 ownership，故非 IDOR、屬 #M1 非 #M2：
 * - CouponController.claimCoupon：memberId = 登入者；service 端 COUPON_NOT_FOUND /
 *   COUPON_EXPIRED / 領完上限 / COUPON_ALREADY_CLAIMED 防護完整
 * - ProductReviewController.addReview：memberId = 登入者
 *
 * ⚠️ 查證衍生：ProductReviewServiceImpl.add 為裸 insert，無「需購買」/「去重」防護
 *    （server 不強制，僅前端 checkReview UI gate）→ 評論灌水/重複評論向量，
 *    非跨用戶 IDOR 故不影響本批 @PreAuthorize 正確性，獨立追蹤見 #M15。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CouponReviewAuthzTest extends AbstractIntegrationTest {

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

    // ── 契約：匿名打 2 個寫入端點一律 401 ──

    @Test
    void couponReviewWritesAnonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/coupon/claim")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/product/review")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ── 沒被過度限制：已認證 USER 通過授權層落到精確下游碼（證明 isAuthenticated 非 ADMIN）──

    @Test
    void claimCoupon_asUser_passesAuthzThenValidation400() throws Exception {
        // 授權通過 → @Valid（ClaimCouponRequest.couponId @NotNull）→ 400，非 401/403
        mockMvc.perform(post("/api/coupon/claim").with(user(seedUser()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReview_asUser_passesAuthzThenValidation400() throws Exception {
        // 授權通過 → @Valid（ReviewRequest.rating @NotNull）→ 400，非 401/403
        mockMvc.perform(post("/api/product/review").with(user(seedUser()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1}"))
                .andExpect(status().isBadRequest());
    }
}
