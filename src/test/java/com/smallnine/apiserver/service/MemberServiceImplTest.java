package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.dto.GoogleProfile;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.exception.ConcurrentOAuthRegistrationException;
import com.smallnine.apiserver.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 純 unit test:focus 在 findOrCreateGoogleUser 的三條分支
 *  1. byGoogleUid 命中 → 直接 return
 *  2. byEmail 命中 → 阻擋未驗證帳號 / 連結已驗證帳號
 *  3. 全新 user → INSERT,並處理 race(DataIntegrityViolationException)
 *
 * 不走 Spring context,避免 @Transactional / DB 真實連線拖慢測試。
 * e2e Google OAuth flow 必須手動瀏覽器測試。
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock private UserDao userDao;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private com.smallnine.apiserver.service.FileStorageService fileStorageService;

    @InjectMocks private MemberServiceImpl memberService;

    private GoogleProfile googleProfile(String sub, String email) {
        return new GoogleProfile(sub, email, "Test User", "https://example.com/pic.jpg");
    }

    @Test
    void findOrCreateGoogleUser_byGoogleUidHit_returnsExistingUser() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("google_abc");
        existing.setEmailValidated(true);

        when(userDao.findByGoogleUid("sub-123")).thenReturn(Optional.of(existing));

        User result = memberService.findOrCreateGoogleUser(googleProfile("sub-123", "foo@example.com"));

        assertThat(result).isSameAs(existing);
        verify(userDao, never()).findByEmail(any());
        verify(userDao, never()).insert(any());
        verify(userDao, never()).update(any());
    }

    @Test
    void findOrCreateGoogleUser_byEmailHit_emailValidatedFalse_throwsAndDoesNotLink() {
        // A1 修補的關鍵測試:既有帳號 emailValidated=false 時禁止 auto-link
        User existing = new User();
        existing.setId(2L);
        existing.setUsername("password_user");
        existing.setEmail("victim@example.com");
        existing.setEmailValidated(false);   // 未驗證

        when(userDao.findByGoogleUid("attacker-sub")).thenReturn(Optional.empty());
        when(userDao.findByEmail("victim@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                memberService.findOrCreateGoogleUser(googleProfile("attacker-sub", "victim@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("尚未驗證");

        // 確認沒被連結:googleUid 不該被寫入,update 不該被呼叫
        verify(userDao, never()).update(any());
        verify(userDao, never()).insert(any());
        assertThat(existing.getGoogleUid()).isNull();
    }

    @Test
    void findOrCreateGoogleUser_byEmailHit_emailValidatedTrue_linksAccount() {
        User existing = new User();
        existing.setId(3L);
        existing.setUsername("verified_user");
        existing.setEmail("verified@example.com");
        existing.setEmailValidated(true);   // 已驗證

        when(userDao.findByGoogleUid("new-sub")).thenReturn(Optional.empty());
        when(userDao.findByEmail("verified@example.com")).thenReturn(Optional.of(existing));

        User result = memberService.findOrCreateGoogleUser(googleProfile("new-sub", "verified@example.com"));

        // googleUid 應被寫入
        assertThat(result.getGoogleUid()).isEqualTo("new-sub");
        assertThat(result.getGoogleName()).isEqualTo("Test User");
        verify(userDao).update(existing);
        verify(userDao, never()).insert(any());
    }

    @Test
    void findOrCreateGoogleUser_newUser_insertsSuccessfully() {
        when(userDao.findByGoogleUid("brand-new-sub")).thenReturn(Optional.empty());
        when(userDao.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        User result = memberService.findOrCreateGoogleUser(googleProfile("brand-new-sub", "new@example.com"));

        assertThat(result.getGoogleUid()).isEqualTo("brand-new-sub");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getEmailValidated()).isTrue();
        assertThat(result.getUsername()).startsWith("google_");
        verify(userDao).insert(result);
    }

    @Test
    void findOrCreateGoogleUser_raceCondition_throwsConcurrentException() {
        // 模擬 race:findByGoogleUid + findByEmail 都查不到(另一個 thread 還沒 commit),
        // 但 INSERT 時撞到 google_uid UNIQUE constraint
        when(userDao.findByGoogleUid("race-sub")).thenReturn(Optional.empty());
        when(userDao.findByEmail("race@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        doThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"))
                .when(userDao).insert(any(User.class));

        assertThatThrownBy(() ->
                memberService.findOrCreateGoogleUser(googleProfile("race-sub", "race@example.com")))
                .isInstanceOf(ConcurrentOAuthRegistrationException.class)
                .extracting("providerSub").isEqualTo("race-sub");
    }

    @Test
    void findOrCreateGoogleUser_missingSub_throwsBadRequest() {
        assertThatThrownBy(() ->
                memberService.findOrCreateGoogleUser(googleProfile(null, "foo@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sub");
    }

    @Test
    void findOrCreateGoogleUser_missingEmail_throwsBadRequest() {
        when(userDao.findByGoogleUid("sub")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                memberService.findOrCreateGoogleUser(googleProfile("sub", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email");
    }
}
