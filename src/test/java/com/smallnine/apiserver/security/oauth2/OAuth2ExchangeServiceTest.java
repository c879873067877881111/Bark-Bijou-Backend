package com.smallnine.apiserver.security.oauth2;

import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.dto.AuthResponse;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.AccountDisabledException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * #NEW-B：OAuth code 兌換也要擋停用帳號（與 #NEW-A 同理）。
 */
@ExtendWith(MockitoExtension.class)
class OAuth2ExchangeServiceTest {

    @Mock private OAuth2CodeStore codeStore;
    @Mock private UserDao userDao;

    @InjectMocks private OAuth2ExchangeService service;

    private OAuth2CodeStore.Payload payload;

    @BeforeEach
    void setUp() {
        payload = new OAuth2CodeStore.Payload(7L, "acc", "ref", System.currentTimeMillis() + 60_000);
        when(codeStore.consume("code-1")).thenReturn(Optional.of(payload));
    }

    @Test
    void disabledAccount_throws() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setEmailValidated(false);
        when(userDao.findById(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.exchange("code-1"))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    void enabledAccount_returnsTokens() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setEmailValidated(true);
        when(userDao.findById(7L)).thenReturn(Optional.of(user));

        AuthResponse resp = service.exchange("code-1");

        assertThat(resp.getAccessToken()).isEqualTo("acc");
        assertThat(resp.getRefreshToken()).isEqualTo("ref");
    }
}
