package com.smallnine.apiserver.utils;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * #H3-revised：JWT secret 的驗證必須在 startup（@PostConstruct）就 fail，
 * 不能拖到第一次簽 token 才炸；且 getBytes 要鎖死 UTF-8，不吃 platform charset。
 */
class JwtUtilSecretValidationTest {

    private JwtUtil newJwtUtil(String secret) {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 604800000L);
        return jwtUtil;
    }

    @Test
    void validSecret_initSucceedsAndCanSignAndVerify() {
        // 32 bytes（256 bit）剛好滿足 HS256 下限
        JwtUtil jwtUtil = newJwtUtil("0123456789abcdef0123456789abcdef");

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(jwtUtil, "initSigningKey"))
                .doesNotThrowAnyException();

        String token = jwtUtil.generateToken("alice");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void nullSecret_failsAtInit() {
        JwtUtil jwtUtil = newJwtUtil(null);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(jwtUtil, "initSigningKey"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");
    }

    @Test
    void blankSecret_failsAtInit() {
        JwtUtil jwtUtil = newJwtUtil("   ");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(jwtUtil, "initSigningKey"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");
    }

    @Test
    void multiByteSecret_judgedByUtf8ByteLengthNotCharCount() {
        // 16 個中日韓字：char 數 16（< 32）但 UTF-8 是 48 bytes（>= 32）。
        // 若誤用 char 數或非 UTF-8 charset 就會錯判，這裡釘死「走 UTF-8 byte 長度」。
        JwtUtil jwtUtil = newJwtUtil("一二三四五六七八九十壹貳參肆伍陸");

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(jwtUtil, "initSigningKey"))
                .doesNotThrowAnyException();

        String token = jwtUtil.generateToken("bob");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("bob");
    }

    @Test
    void shortSecret_failsAtInitWithLength() {
        // 31 bytes，差 1 byte 不到 256 bit
        JwtUtil jwtUtil = newJwtUtil("0123456789abcdef0123456789abcde");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(jwtUtil, "initSigningKey"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
