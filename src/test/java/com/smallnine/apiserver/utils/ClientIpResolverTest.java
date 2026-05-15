package com.smallnine.apiserver.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * #H2：clientIp 信任邊界（取代 #35 controller 內的舊邏輯）。
 * 重點錯誤路徑：偽造 X-Forwarded-For 不能繞過 per-IP 限流。
 */
class ClientIpResolverTest {

    private ClientIpResolver resolver;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        resolver = new ClientIpResolver();
        request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.9");
    }

    /** 預設不信任：偽造 header 完全忽略 */
    @Test
    void ignoresForwardedHeaderByDefault() {
        ReflectionTestUtils.setField(resolver, "trustForwardedFor", false);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");
    }

    /**
     * 信任時取「最右側」hop——nginx append 後 header 是
     * {client 偽造的, 反代看到的真實 IP}，最右才是不可偽造的那個。
     * 取最左（#35 舊行為）會被 client 騙。
     */
    @Test
    void trustsRightmostHopNotSpoofedLeftmost() {
        ReflectionTestUtils.setField(resolver, "trustForwardedFor", true);
        when(request.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9, 5.6.7.8");

        assertThat(resolver.resolve(request)).isEqualTo("5.6.7.8");
    }

    /** 信任但無 header：回退 remoteAddr */
    @Test
    void fallsBackToRemoteAddrWhenHeaderMissing() {
        ReflectionTestUtils.setField(resolver, "trustForwardedFor", true);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");
    }

    /** 信任但 header 尾段是空白：跳過空白往左取第一個有效值 */
    @Test
    void skipsBlankTrailingHops() {
        ReflectionTestUtils.setField(resolver, "trustForwardedFor", true);
        when(request.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9, 5.6.7.8, ");

        assertThat(resolver.resolve(request)).isEqualTo("5.6.7.8");
    }
}
