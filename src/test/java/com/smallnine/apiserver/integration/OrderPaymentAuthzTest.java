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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Autowired private MockMvc mockMvc;

    private UserPrincipal seedUser() {
        User u = new User();
        u.setId(1L);
        u.setUsername("authz-user");
        u.setRole(User.Role.USER);
        u.setEmailValidated(true);
        return new UserPrincipal(u);
    }

    private void assertNotAuthBlocked(MvcResult r) {
        int s = r.getResponse().getStatus();
        assertTrue(s != 401 && s != 403,
                "已認證 USER 不該被授權層擋下，實際狀態=" + s);
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

    // ── 沒被過度限制：已認證 USER 通過授權層（下游業務錯誤如 400/404 可接受）──

    @Test
    void createOrder_asUser_passesAuthz() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/orders").with(user(seedUser()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();
        assertNotAuthBlocked(r);
    }

    @Test
    void cancelOrder_asUser_passesAuthz() throws Exception {
        MvcResult r = mockMvc.perform(put("/api/orders/999999/cancel").with(user(seedUser())))
                .andReturn();
        assertNotAuthBlocked(r);
    }

    @Test
    void paymentCreateOrder_asUser_passesAuthz() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/ecpay/create-order").with(user(seedUser()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();
        assertNotAuthBlocked(r);
    }
}
