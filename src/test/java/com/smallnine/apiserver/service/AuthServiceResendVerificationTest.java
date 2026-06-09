package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.logging.AuditLogger;
import com.smallnine.apiserver.service.impl.AuthServiceImpl;
import com.smallnine.apiserver.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * #H1 防帳號枚舉：resendVerificationEmail 對「信箱不存在 / 已驗證」
 * 必須靜默結束，不得拋例外、不得寄信，避免回應洩漏信箱是否註冊。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceResendVerificationTest {

    @Mock
    private UserDao userDao;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private MailService mailService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void unknownEmail_doesNotThrow_andDoesNotSendMail() {
        when(userDao.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatCode(() -> authService.resendVerificationEmail("ghost@example.com"))
                .doesNotThrowAnyException();

        verify(mailService, never()).sendVerificationEmail(anyString(), anyString());
        verify(userDao, never()).update(any());
    }

    @Test
    void alreadyValidatedEmail_doesNotThrow_andDoesNotSendMail() {
        User user = new User();
        user.setEmail("done@example.com");
        user.setEmailValidated(true);
        when(userDao.findByEmail("done@example.com")).thenReturn(Optional.of(user));

        assertThatCode(() -> authService.resendVerificationEmail("done@example.com"))
                .doesNotThrowAnyException();

        verify(mailService, never()).sendVerificationEmail(anyString(), anyString());
        verify(userDao, never()).update(any());
    }

    @Test
    void unverifiedEmail_sendsMailExactlyOnce() {
        User user = new User();
        user.setEmail("pending@example.com");
        user.setEmailValidated(false);
        when(userDao.findByEmail("pending@example.com")).thenReturn(Optional.of(user));

        authService.resendVerificationEmail("pending@example.com");

        verify(userDao, times(1)).update(user);
        verify(mailService, times(1)).sendVerificationEmail(anyString(), anyString());
    }

    /** 壞輸入：空字串信箱（@RequestParam 可傳空）不得拋例外、不得寄信 */
    @Test
    void blankEmail_handledGracefully() {
        when(userDao.findByEmail("")).thenReturn(Optional.empty());

        assertThatCode(() -> authService.resendVerificationEmail(""))
                .doesNotThrowAnyException();

        verify(mailService, never()).sendVerificationEmail(anyString(), anyString());
        verify(userDao, never()).update(any());
    }

    /** 錯誤路徑：寄信失敗時例外必須往外傳，讓 @Transactional 回滾 token 寫入 */
    @Test
    void mailSendFailure_propagatesException() {
        User user = new User();
        user.setEmail("pending@example.com");
        user.setEmailValidated(false);
        when(userDao.findByEmail("pending@example.com")).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("SMTP down"))
                .when(mailService).sendVerificationEmail(anyString(), anyString());

        assertThatThrownBy(() -> authService.resendVerificationEmail("pending@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SMTP down");
    }
}