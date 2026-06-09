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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #M1 批次2：Order(createOrder/cancelOrder) + Payment(create-order) 寫入端點授權紀律。
 *
 * 這三個端點在現行 SecurityConfig `.anyRequest().authenticated()` 下匿名本就 401，
 * 補 @PreAuthorize("isAuthenticated()") 是把隱含授權變顯式 + defense-in-depth：
 * 即使日後白名單誤開，方法層仍擋。因此測試是「契約守門」而非紅→綠：
 *   - 匿名 → 401（契約：寫入必須認證）
 *   - 已認證 USER → 非 401/403（沒被過度限制，例如沒誤寫成 hasRole('ADMIN')）
 *
 * ecpay /callback 是金流閘道 webhook，刻意不掛 @PreAuthorize（見 REVIEW_TODO #M11）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class OrderPaymentAuthzTest extends AbstractIntegrationTest {

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

    // ── 契約：匿名一律 401 ──

    @Test
    void writesAnonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/orders/1/cancel"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/ecpay/create-order")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ── 沒被過度限制：已認證 USER 通過授權層，落到下游業務（精確下游碼，非 401/403）──
    // 註：跨租戶 ownership（USER A 取消 USER B 訂單 → 403）由 OrderServiceTest.cancelOrder_notOwner
    // 覆蓋；本檔只驗 #M1 的 authz 註解契約，不重複測 ownership 行為。

    @Test
    void createOrder_asUser_passesAuthzThenValidation400() throws Exception {
        // 授權通過 → 落到 @Valid（空 body 缺必填）→ 400，而非 401/403
        mockMvc.perform(post("/api/orders").with(user(seedUser()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelOrder_asUser_passesAuthzThenNotFound404() throws Exception {
        // 授權通過 → 訂單不存在 → 404，而非 401/403
        mockMvc.perform(put("/api/orders/999999/cancel").with(user(seedUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void paymentCreateOrder_asUser_passesAuthzThenValidation400() throws Exception {
        // 授權通過 → @Valid（缺 orderId/totalAmount）→ 400，而非 401/403
        mockMvc.perform(post("/api/ecpay/create-order").with(user(seedUser()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
