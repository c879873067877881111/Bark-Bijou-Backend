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
}
