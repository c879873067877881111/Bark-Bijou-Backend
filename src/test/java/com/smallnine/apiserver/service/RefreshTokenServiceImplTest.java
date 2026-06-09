package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dao.RefreshTokenDao;
import com.smallnine.apiserver.entity.RefreshToken;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.impl.RefreshTokenServiceImpl;
import com.smallnine.apiserver.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * #C2 token rotation 行為驗證:
 *  - login / register / OAuth (rotateFromToken=null) 不撤銷任何既有 token
 *  - refresh (rotateFromToken=舊顆) 只撤銷那一顆
 *  - 多裝置:A 裝置登入後 B 裝置登入,A 裝置的 token 必須留著
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenDao refreshTokenDao;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private RefreshTokenServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
        user.setUsername("alice");
    }

    private void stubExpiration() {
        // 只給會 createRefreshToken 的測試呼叫,避免 Mockito strict stubbing 抓到未使用
        when(jwtUtil.getRefreshTokenExpirationTime()).thenReturn(7L * 24 * 60 * 60 * 1000);
    }

    @Test
    void createRefreshToken_nullRotateFrom_doesNotRevokeAnything() {
        stubExpiration();
        RefreshToken issued = service.createRefreshToken(user, null);

        assertThat(issued).isNotNull();
        assertThat(issued.getUserId()).isEqualTo(42L);
        assertThat(issued.getToken()).isNotBlank();
        assertThat(issued.getRevoked()).isFalse();

        verify(refreshTokenDao).save(any(RefreshToken.class));
        // 關鍵:沒有任何撤銷動作
        verify(refreshTokenDao, never()).revokeByToken(any());
        verify(refreshTokenDao, never()).revokeByUserId(any());
    }

    @Test
    void createRefreshToken_withRotateFrom_revokesOnlyThatToken() {
        stubExpiration();
        String oldToken = "old-refresh-uuid";

        service.createRefreshToken(user, oldToken);

        // 只撤銷指定那一顆
        verify(refreshTokenDao, times(1)).revokeByToken(oldToken);
        verify(refreshTokenDao, never()).revokeByUserId(any());
        verify(refreshTokenDao).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_multiDeviceLogin_doesNotKickOtherDevices() {
        stubExpiration();
        // 模擬:裝置 A 已登入持有 tokenA;現在裝置 B 也登入
        // 裝置 B 的 login 不該影響 tokenA
        ArgumentCaptor<RefreshToken> savedTokens = ArgumentCaptor.forClass(RefreshToken.class);

        RefreshToken tokenForA = service.createRefreshToken(user, null);
        RefreshToken tokenForB = service.createRefreshToken(user, null);

        verify(refreshTokenDao, times(2)).save(savedTokens.capture());
        verify(refreshTokenDao, never()).revokeByToken(any());
        verify(refreshTokenDao, never()).revokeByUserId(any());

        // 兩顆 token 不同字串
        assertThat(tokenForA.getToken()).isNotEqualTo(tokenForB.getToken());
        assertThat(savedTokens.getAllValues())
                .extracting(RefreshToken::getToken)
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }

    @Test
    void revokeByUser_explicitAdminAction_stillRevokesAll() {
        // 主動撤銷該 user 全部裝置(admin 強制登出 / 改密碼) 仍要保留
        service.revokeByUser(user);

        verify(refreshTokenDao).revokeByUserId(42L);
    }
}
