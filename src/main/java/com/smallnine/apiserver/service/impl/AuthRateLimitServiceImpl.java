package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.AuthRateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * #H2 認證端點分散式限流（Redis 計數）。
 *
 * 刻意不引 bucket4j-redis：本專案既有 Redis 用法都是 RedisTemplate
 * （見 OAuth2CodeStore / OrderServiceImpl），INCR+EXPIRE 對「暴力破解
 * 防護 / 失敗計數 / 鎖定」已足夠，少一個依賴、好 mock 測。
 *
 * 失敗達門檻就寫一支「lock key」，前置檢查只用 hasKey 判斷——不回讀
 * INCR 的數值，避開 value 序列化器（JSON）反序列化純數字字串的雷。
 *
 * **fail-open**：Redis 故障時一律放行並記 warn——限流器壞掉不能把所有人
 * 擋在登入外（可用性優先於限流）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthRateLimitServiceImpl implements AuthRateLimitService {

    // 單一帳號連續失敗門檻 / 計數視窗 / 鎖定時長
    private static final long USERNAME_FAIL_LIMIT = 5;
    private static final Duration USERNAME_FAIL_WINDOW = Duration.ofMinutes(15);
    private static final Duration USERNAME_LOCK = Duration.ofMinutes(15);
    // 單一 IP 在視窗內的登入失敗上限（擋從同 IP 的帳號噴灑）
    private static final long IP_FAIL_LIMIT = 20;
    private static final Duration IP_FAIL_WINDOW = Duration.ofMinutes(15);
    private static final Duration IP_LOCK = Duration.ofMinutes(15);

    private static final String K_USER_FAIL = "rl:login:userfail:";
    private static final String K_USER_LOCK = "rl:login:lock:";
    private static final String K_IP_FAIL = "rl:login:ipfail:";
    private static final String K_IP_LOCK = "rl:login:iplock:";
    private static final String K_IP_QUOTA = "rl:ip:";

    private final RedisTemplate<String, Object> redisTemplate;

    // ---- 登入失敗保護 ----

    @Override
    public void assertLoginAllowed(String usernameOrEmail, String ip) {
        String user = norm(usernameOrEmail);
        try {
            if (locked(K_USER_LOCK + user)) {
                log.warn("action=login user={} ip={} result=blocked reason=account_locked", user, ip);
                throw new BusinessException(ResponseCode.TOO_MANY_REQUESTS,
                        "嘗試次數過多，帳號已暫時鎖定，請 15 分鐘後再試");
            }
            if (locked(K_IP_LOCK + ip)) {
                log.warn("action=login ip={} result=blocked reason=ip_locked", ip);
                throw new BusinessException(ResponseCode.TOO_MANY_REQUESTS, "嘗試次數過多，請稍後再試");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("action=login result=ratelimit_degraded reason=redis_unavailable msg={}", e.getMessage());
        }
    }

    @Override
    public void recordLoginFailure(String usernameOrEmail, String ip) {
        String user = norm(usernameOrEmail);
        try {
            long userFails = bumpSliding(K_USER_FAIL + user, USERNAME_FAIL_WINDOW);
            if (userFails >= USERNAME_FAIL_LIMIT) {
                redisTemplate.opsForValue().set(K_USER_LOCK + user, "1", USERNAME_LOCK);
                log.warn("action=login user={} result=locked fail_count={}", user, userFails);
            }
            long ipFails = bumpSliding(K_IP_FAIL + ip, IP_FAIL_WINDOW);
            if (ipFails >= IP_FAIL_LIMIT) {
                redisTemplate.opsForValue().set(K_IP_LOCK + ip, "1", IP_LOCK);
                log.warn("action=login ip={} result=locked fail_count={}", ip, ipFails);
            }
        } catch (RuntimeException e) {
            log.warn("action=login result=ratelimit_degraded reason=redis_unavailable msg={}", e.getMessage());
        }
    }

    @Override
    public void recordLoginSuccess(String usernameOrEmail) {
        String user = norm(usernameOrEmail);
        try {
            redisTemplate.delete(K_USER_FAIL + user);
            redisTemplate.delete(K_USER_LOCK + user);
        } catch (RuntimeException e) {
            log.warn("action=login result=ratelimit_degraded reason=redis_unavailable msg={}", e.getMessage());
        }
    }

    // ---- 一般 per-IP 固定視窗 throttle（register / oauth-exchange 等）----

    @Override
    public void assertIpQuota(String action, String ip, long limit, Duration window) {
        String key = K_IP_QUOTA + action + ":" + ip;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, window);
            }
            if (count != null && count > limit) {
                log.warn("action={} ip={} result=blocked reason=ip_quota count={}", action, ip, count);
                throw new BusinessException(ResponseCode.TOO_MANY_REQUESTS, "請求過於頻繁，請稍後再試");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("action={} result=ratelimit_degraded reason=redis_unavailable msg={}", action, e.getMessage());
        }
    }

    // ---- 內部 ----

    /** 滑動視窗：每次命中都把計數 +1 並把 TTL 重設為 window（持續攻擊就持續鎖）。 */
    private long bumpSliding(String key, Duration window) {
        Long c = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, window);
        return c == null ? 0L : c;
    }

    private boolean locked(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}