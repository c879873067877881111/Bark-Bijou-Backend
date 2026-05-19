package com.smallnine.apiserver.service;

/**
 * #H1 認證端點限流（純記憶體 token bucket）。
 *
 * 僅適用單機；多機 / 分散式限流（登入端點）見 {@link AuthRateLimitService}。
 * 實作見 {@link com.smallnine.apiserver.service.impl.RateLimiterServiceImpl}。
 */
public interface RateLimiterService {

    /**
     * 嘗試為一次 resend-verification 請求扣 token。
     * email 與 IP 任一超限即視為被限流。
     *
     * @return true=放行；false=已超限
     */
    boolean tryResendVerification(String email, String clientIp);
}