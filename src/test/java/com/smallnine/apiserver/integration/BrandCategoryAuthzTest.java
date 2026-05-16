package com.smallnine.apiserver.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #M1 批次1：Brand / Category 寫入端點授權紀律。
 * 規則：所有寫入端點必須 @PreAuthorize("hasRole('ADMIN')")。
 * 涵蓋錯誤 case（USER 一律 403）與正常 case（ADMIN 建立成功）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class BrandCategoryAuthzTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    // ── 錯誤 case：一般使用者（非 ADMIN）對寫入端點一律 403 ──

    @Test
    void brandWrites_asUser_forbidden() throws Exception {
        String body = json(Map.of("name", " test-brand"));

        mockMvc.perform(post("/api/brands").with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/brands/1").with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/brands/1").with(user("u").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void categoryWrites_asUser_forbidden() throws Exception {
        String body = json(Map.of("name", "test-category"));

        mockMvc.perform(post("/api/categories").with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/categories/1").with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/categories/1").with(user("u").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/categories/1/status").param("isActive", "false")
                        .with(user("u").roles("USER")))
                .andExpect(status().isForbidden());
    }

    // ── 正常 case：ADMIN 可建立（授權通過，非 403/401）──

    @Test
    void brandCreate_asAdmin_created() throws Exception {
        mockMvc.perform(post("/api/brands").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "admin-brand"))))
                .andExpect(status().isCreated());
    }

    @Test
    void categoryCreate_asAdmin_created() throws Exception {
        mockMvc.perform(post("/api/categories").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "admin-category"))))
                .andExpect(status().isCreated());
    }
}
