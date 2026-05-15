package com.smallnine.apiserver.service;

import com.smallnine.apiserver.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * #H2 分散式限流。涵蓋正常 + 錯誤路徑：
 *  - 帳號達門檻鎖定 / 未達門檻不鎖
 *  - 鎖定 / IP 鎖定時前置檢查擋下
 *  - 登入成功清計數
 *  - IP quota 超限丟 429
 *  - **Redis 故障 fail-open**：所有方法都不得拋例外
 */
@ExtendWith(MockitoExtension.class)
class AuthRateLimitServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;

    private AuthRateLimitService svc;

    @BeforeEach
    void setUp() {
        svc = new AuthRateLimitService(redisTemplate);
    }

    @Test
    void recordLoginFailure_locksAccountWhenThresholdReached() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // 第 5 次帳號失敗 → 達門檻；IP 第 1 次 → 不鎖
        when(valueOps.increment("rl:login:userfail:alice")).thenReturn(5L);
        when(valueOps.increment("rl:login:ipfail:1.1.1.1")).thenReturn(1L);

        svc.recordLoginFailure("Alice", "1.1.1.1");

        verify(valueOps).set(eq("rl:login:lock:alice"), eq("1"), any(Duration.class));
        verify(valueOps, never()).set(eq("rl:login:iplock:1.1.1.1"), any(), any(Duration.class));
    }

    @Test
    void recordLoginFailure_doesNotLockBelowThreshold() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rl:login:userfail:bob")).thenReturn(2L);
        when(valueOps.increment("rl:login:ipfail:2.2.2.2")).thenReturn(2L);

        svc.recordLoginFailure("bob", "2.2.2.2");

        verify(valueOps, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void assertLoginAllowed_blocksWhenAccountLocked() {
        when(redisTemplate.hasKey("rl:login:lock:alice")).thenReturn(true);

        assertThatThrownBy(() -> svc.assertLoginAllowed("alice", "9.9.9.9"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void assertLoginAllowed_blocksWhenIpLocked() {
        when(redisTemplate.hasKey("rl:login:lock:carol")).thenReturn(false);
        when(redisTemplate.hasKey("rl:login:iplock:3.3.3.3")).thenReturn(true);

        assertThatThrownBy(() -> svc.assertLoginAllowed("carol", "3.3.3.3"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void assertLoginAllowed_passesWhenNoLocks() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        assertThatCode(() -> svc.assertLoginAllowed("dave", "4.4.4.4"))
                .doesNotThrowAnyException();
    }

    @Test
    void recordLoginSuccess_clearsFailCountAndLock() {
        svc.recordLoginSuccess("Eve");

        verify(redisTemplate).delete("rl:login:userfail:eve");
        verify(redisTemplate).delete("rl:login:lock:eve");
    }

    @Test
    void assertIpQuota_throwsWhenOverLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rl:ip:register:5.5.5.5")).thenReturn(6L);

        assertThatThrownBy(() -> svc.assertIpQuota("register", "5.5.5.5", 5, Duration.ofMinutes(10)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void assertIpQuota_setsTtlOnFirstHit_andPassesWithinLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rl:ip:register:6.6.6.6")).thenReturn(1L);

        assertThatCode(() -> svc.assertIpQuota("register", "6.6.6.6", 5, Duration.ofMinutes(10)))
                .doesNotThrowAnyException();

        verify(redisTemplate).expire(eq("rl:ip:register:6.6.6.6"), any(Duration.class));
    }

    // ---- fail-open：Redis 炸了所有路徑都要放行，不能把人擋在登入外 ----

    @Test
    void failsOpenWhenRedisDownOnAssertLogin() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));

        assertThatCode(() -> svc.assertLoginAllowed("alice", "1.1.1.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void failsOpenWhenRedisDownOnRecordFailure() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        assertThatCode(() -> svc.recordLoginFailure("alice", "1.1.1.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void failsOpenWhenRedisDownOnIpQuota() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        assertThatCode(() -> svc.assertIpQuota("register", "1.1.1.1", 5, Duration.ofMinutes(10)))
                .doesNotThrowAnyException();
    }

    @Test
    void failsOpenWhenRedisDownOnRecordSuccess() {
        lenient().doThrow(new RuntimeException("redis down")).when(redisTemplate).delete(anyString());

        assertThatCode(() -> svc.recordLoginSuccess("alice"))
                .doesNotThrowAnyException();
    }
}
