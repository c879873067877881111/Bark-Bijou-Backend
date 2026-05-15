package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.dto.AuthResponse;
import com.smallnine.apiserver.dto.RefreshTokenRequest;
import com.smallnine.apiserver.entity.RefreshToken;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.AccountDisabledException;
import com.smallnine.apiserver.logging.AuditLogger;
import com.smallnine.apiserver.service.impl.AuthServiceImpl;
import com.smallnine.apiserver.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * #NEW-A：refresh 流程必須 reload User 並擋停用帳號。
 * admin 在 DB 把 email_validated 設 false 後，該 user 不能再靠手上的
 * refresh token 換新 access token，且全部裝置 session 一併撤銷。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTokenTest {

    @Mock private UserDao userDao;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuditLogger auditLogger;
    @Mock private MailService mailService;

    @InjectMocks private AuthServiceImpl authService;

    private RefreshTokenRequest request;
    private RefreshToken storedToken;

    @BeforeEach
    void setUp() {
        request = new RefreshTokenRequest();
        request.setRefreshToken("rt-old");

        storedToken = new RefreshToken();
        storedToken.setToken("rt-old");
        storedToken.setUserId(7L);

        when(refreshTokenService.findByToken("rt-old")).thenReturn(Optional.of(storedToken));
        when(refreshTokenService.verifyExpiration(storedToken)).thenReturn(storedToken);
    }

    @Test
    void disabledAccount_throwsAndRevokesAllSessions() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setEmailValidated(false); // admin 停用
        when(userDao.findById(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AccountDisabledException.class);

        verify(refreshTokenService).revokeByUser(user);
        verify(refreshTokenService, never()).createRefreshToken(any(), anyString());
    }

    @Test
    void enabledAccount_issuesNewTokens() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setEmailValidated(true);
        when(userDao.findById(7L)).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken("alice")).thenReturn("new-access");
        when(jwtUtil.getAccessTokenExpirationTime()).thenReturn(900_000L);
        RefreshToken rotated = new RefreshToken();
        rotated.setToken("rt-new");
        when(refreshTokenService.createRefreshToken(user, "rt-old")).thenReturn(rotated);

        AuthResponse resp = authService.refreshToken(request);

        assertThat(resp.getAccessToken()).isEqualTo("new-access");
        assertThat(resp.getRefreshToken()).isEqualTo("rt-new");
        verify(refreshTokenService, never()).revokeByUser(any());
    }
}
