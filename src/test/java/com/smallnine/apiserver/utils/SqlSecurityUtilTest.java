package com.smallnine.apiserver.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlSecurityUtilTest {

    @Test
    void escapeLikePattern_withNull_returnsNull() {
        assertThat(SqlSecurityUtil.escapeLikePattern(null)).isNull();
    }

    @Test
    void escapeLikePattern_withNormalString_returnsUnchanged() {
        assertThat(SqlSecurityUtil.escapeLikePattern("hello")).isEqualTo("hello");
    }

    @Test
    void escapeLikePattern_withPercent_escapesPercent() {
        assertThat(SqlSecurityUtil.escapeLikePattern("50%")).isEqualTo("50\\%");
    }

    @Test
    void escapeLikePattern_withUnderscore_escapesUnderscore() {
        assertThat(SqlSecurityUtil.escapeLikePattern("user_name")).isEqualTo("user\\_name");
    }

    @Test
    void escapeLikePattern_withBackslash_escapesBackslash() {
        assertThat(SqlSecurityUtil.escapeLikePattern("path\\file")).isEqualTo("path\\\\file");
    }

    @Test
    void escapeLikePattern_withAllSpecialChars_escapesAll() {
        assertThat(SqlSecurityUtil.escapeLikePattern("100%_test\\")).isEqualTo("100\\%\\_test\\\\");
    }

    @Test
    void sanitizeInput_withNull_returnsNull() {
        assertThat(SqlSecurityUtil.sanitizeInput(null, 100)).isNull();
    }

    @Test
    void sanitizeInput_withNormalString_returnsUnchanged() {
        assertThat(SqlSecurityUtil.sanitizeInput("hello", 100)).isEqualTo("hello");
    }

    @Test
    void sanitizeInput_withDangerousChars_removesChars() {
        assertThat(SqlSecurityUtil.sanitizeInput("<script>alert('xss')</script>", 100))
                .isEqualTo("scriptalert(xss)/script");
    }

    @Test
    void sanitizeInput_exceedsMaxLength_truncates() {
        assertThat(SqlSecurityUtil.sanitizeInput("hello world", 5)).isEqualTo("hello");
    }

    @Test
    void sanitizeInput_withEmptyAfterCleaning_returnsNull() {
        assertThat(SqlSecurityUtil.sanitizeInput("<>\"'\\", 100)).isNull();
    }

    @Test
    void sanitizeInput_withWhitespace_trims() {
        assertThat(SqlSecurityUtil.sanitizeInput("  hello  ", 100)).isEqualTo("hello");
    }
}
