package com.smallnine.apiserver.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

/**
 * #H1 認證端點限流（純記憶體 token bucket）。
 *
 * bucket 存在 Caffeine 有界 cache：
 *  - key 是攻擊者可控的 email / IP，必須有上限，否則限流器本身會被塞爆記憶體
 *  - expireAfterAccess 設為各自的補滿視窗：閒置滿一個視窗的 entry 即使被淘汰，
 *    重建也是滿桶——反正它本來就已被 refill 補滿，語意安全；maximumSize 則是硬上限
 *
 * 僅適用單機；多機 / 分散式限流（登入端點）留待 #H2 改 Redis-backed。
 * 此處故意只覆蓋 resend-verification 一條路徑，不做過度設計。
 */
@Service
public class RateLimiterService {

    private static final long MAX_TRACKED_KEYS = 100_000;
    private static final Duration EMAIL_WINDOW = Duration.ofMinutes(5);
    private static final Duration IP_WINDOW = Duration.ofMinutes(1);

    /** 每個 email：5 分鐘只准 1 次重寄 */
    private final Cache<String, Bucket> resendVerificationByEmail = Caffeine.newBuilder()
            .maximumSize(MAX_TRACKED_KEYS)
            .expireAfterAccess(EMAIL_WINDOW)
            .build();

    /** 每個 IP：每分鐘最多 5 次（擋整段 IP 掃信箱） */
    private final Cache<String, Bucket> resendVerificationByIp = Caffeine.newBuilder()
            .maximumSize(MAX_TRACKED_KEYS)
            .expireAfterAccess(IP_WINDOW)
            .build();

    private Bucket newEmailBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(1, Refill.intervally(1, EMAIL_WINDOW)))
                .build();
    }

    private Bucket newIpBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(5, IP_WINDOW)))
                .build();
    }

    /**
     * 嘗試為一次 resend-verification 請求扣 token。
     * email 與 IP 任一超限即視為被限流。
     *
     * @return true=放行；false=已超限
     */
    public boolean tryResendVerification(String email, String clientIp) {
        boolean ipOk = resendVerificationByIp
                .get(clientIp, k -> newIpBucket())
                .tryConsume(1);
        if (!ipOk) {
            return false;
        }
        return resendVerificationByEmail
                .get(email.toLowerCase(Locale.ROOT), k -> newEmailBucket())
                .tryConsume(1);
    }
}
