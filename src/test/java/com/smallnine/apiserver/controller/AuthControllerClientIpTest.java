package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.security.oauth2.OAuth2ExchangeService;
import com.smallnine.apiserver.service.AuthService;
import com.smallnine.apiserver.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * #H1 fast-follow：clientIp() 的 X-Forwarded-For 信任邊界。
 *
 * 重點是錯誤路徑——預設不信任時，偽造的 X-Forwarded-For 必須被忽略，
 * 否則 per-IP 限流可被一行 header 繞過。
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerClientIpTest {

    @Mock private AuthService authService;
    @Mock private OAuth2ExchangeService oAuth2ExchangeService;
    @Mock private RateLimiterService rateLimiterService;

    private MockMvc mockMvc;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, oAuth2ExchangeService, rateLimiterService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(rateLimiterService.tryResendVerification(anyString(), anyString())).thenReturn(true);
    }

    private String capturedIp() throws Exception {
        ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(rateLimiterService)
                .tryResendVerification(eq("a@example.com"), ipCaptor.capture());
        return ipCaptor.getValue();
    }

    /** 預設 false：偽造的 X-Forwarded-For 必須被忽略，用 remoteAddr */
    @Test
    void doesNotTrustForwardedHeaderByDefault() throws Exception {
        ReflectionTestUtils.setField(controller, "trustForwardedFor", false);

        mockMvc.perform(post("/api/auth/resend-verification")
                        .param("email", "a@example.com")
                        .header("X-Forwarded-For", "1.2.3.4")
                        .with(req -> { req.setRemoteAddr("203.0.113.9"); return req; }))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        assertThat(capturedIp()).isEqualTo("203.0.113.9");
    }

    /** 開啟信任時：採信 X-Forwarded-For 第一段 */
    @Test
    void trustsFirstForwardedHopWhenEnabled() throws Exception {
        ReflectionTestUtils.setField(controller, "trustForwardedFor", true);

        mockMvc.perform(post("/api/auth/resend-verification")
                        .param("email", "a@example.com")
                        .header("X-Forwarded-For", "1.2.3.4, 5.6.7.8")
                        .with(req -> { req.setRemoteAddr("203.0.113.9"); return req; }))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        assertThat(capturedIp()).isEqualTo("1.2.3.4");
    }

    /** 開啟信任但沒有 header：回退 remoteAddr */
    @Test
    void fallsBackToRemoteAddrWhenEnabledButHeaderMissing() throws Exception {
        ReflectionTestUtils.setField(controller, "trustForwardedFor", true);

        mockMvc.perform(post("/api/auth/resend-verification")
                        .param("email", "a@example.com")
                        .with(req -> { req.setRemoteAddr("203.0.113.9"); return req; }))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        assertThat(capturedIp()).isEqualTo("203.0.113.9");
    }
}
