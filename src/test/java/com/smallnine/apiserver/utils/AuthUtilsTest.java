package com.smallnine.apiserver.utils;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthUtilsTest {

    @Test
    void getAuthenticatedUser_withValidUser_returnsUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        User result = AuthUtils.getAuthenticatedUser(user);

        assertThat(result).isSameAs(user);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getAuthenticatedUser_withNull_throwsUnauthorized() {
        assertThatThrownBy(() -> AuthUtils.getAuthenticatedUser(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(ResponseCode.UNAUTHORIZED.getCode());
                });
    }
}
