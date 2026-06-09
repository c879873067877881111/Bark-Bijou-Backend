package com.smallnine.apiserver.service;

import java.time.Duration;

/**
 * 認證端點分散式限流（Redis 計數）。
 *
 * 提供登入失敗保護（帳號/IP 計數與鎖定）與一般 per-IP 固定視窗 throttle。
 * 實作見 {@link com.smallnine.apiserver.service.impl.AuthRateLimitServiceImpl}。
 */
public interface AuthRateLimitService {

    /** 登入前置檢查：帳號被鎖、或該 IP 失敗爆表被鎖，直接擋。 */
    void assertLoginAllowed(String usernameOrEmail, String ip);

    /** 登入失敗：累加帳號 + IP 計數，任一達門檻則寫對應 lock key。 */
    void recordLoginFailure(String usernameOrEmail, String ip);

    /** 登入成功：清掉該帳號的失敗計數與鎖定（IP 計數保留，視窗自然過期）。 */
    void recordLoginSuccess(String usernameOrEmail);

    /** 該 IP 對某 action 在固定視窗內超過 limit 即丟 429。Redis 故障時放行。 */
    void assertIpQuota(String action, String ip, long limit, Duration window);
}