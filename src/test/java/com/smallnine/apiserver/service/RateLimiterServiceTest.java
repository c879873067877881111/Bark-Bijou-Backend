package com.smallnine.apiserver.service;

import com.smallnine.apiserver.service.impl.RateLimiterServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #H1 限流行為：
 *  - 每個 email 5 分鐘只准 1 次
 *  - 每個 IP 每分鐘最多 5 次
 */
class RateLimiterServiceTest {

    @Test
    void sameEmail_secondAttemptBlocked_evenFromDifferentIp() {
        RateLimiterService svc = new RateLimiterServiceImpl();

        assertThat(svc.tryResendVerification("a@example.com", "1.1.1.1")).isTrue();
        assertThat(svc.tryResendVerification("a@example.com", "1.1.1.2")).isFalse();
    }

    @Test
    void sameIp_blockedAfterFiveDistinctEmails() {
        RateLimiterService svc = new RateLimiterServiceImpl();
        String ip = "2.2.2.2";

        for (int i = 1; i <= 5; i++) {
            assertThat(svc.tryResendVerification("user" + i + "@example.com", ip))
                    .as("attempt %d should pass", i)
                    .isTrue();
        }
        assertThat(svc.tryResendVerification("user6@example.com", ip)).isFalse();
    }

    /** 錯誤路徑：被擋的 email 立刻重試仍然被擋（額度未回補） */
    @Test
    void blockedEmail_staysBlockedOnImmediateRetry() {
        RateLimiterService svc = new RateLimiterServiceImpl();

        assertThat(svc.tryResendVerification("b@example.com", "3.3.3.3")).isTrue();
        assertThat(svc.tryResendVerification("b@example.com", "3.3.3.4")).isFalse();
        assertThat(svc.tryResendVerification("b@example.com", "3.3.3.5")).isFalse();
    }

    /** 邊界：一個 IP 用爆不應影響另一個 IP */
    @Test
    void exhaustingOneIp_doesNotAffectAnotherIp() {
        RateLimiterService svc = new RateLimiterServiceImpl();
        for (int i = 1; i <= 5; i++) {
            svc.tryResendVerification("x" + i + "@example.com", "4.4.4.4");
        }
        assertThat(svc.tryResendVerification("x6@example.com", "4.4.4.4")).isFalse();
        assertThat(svc.tryResendVerification("y1@example.com", "5.5.5.5")).isTrue();
    }

    /**
     * 回歸：bucket 容器換成有界 Caffeine cache 後，灌入遠超上限的不同 key
     * 不能爆掉、也不能讓限流失效——被鎖定的 email 仍要被擋。
     */
    @Test
    void survivesHighCardinalityChurn_andStillLimits() {
        RateLimiterService svc = new RateLimiterServiceImpl();

        assertThat(svc.tryResendVerification("victim@example.com", "9.9.9.9")).isTrue();

        // 灌 150k 個不同 email / IP（超過 maximumSize 100k），逼 Caffeine 淘汰
        for (int i = 0; i < 150_000; i++) {
            svc.tryResendVerification("noise" + i + "@example.com", "10.0." + (i % 256) + "." + (i % 7));
        }

        // 被鎖的 email 即使中間湧入大量雜訊，仍必須被擋（限流沒因淘汰而失效）
        assertThat(svc.tryResendVerification("victim@example.com", "8.8.8.8")).isFalse();
    }
}