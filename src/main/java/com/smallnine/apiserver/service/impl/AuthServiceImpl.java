package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dto.*;
import com.smallnine.apiserver.entity.RefreshToken;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.AccountDisabledException;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.exception.DuplicateResourceException;
import com.smallnine.apiserver.exception.ResourceNotFoundException;
import com.smallnine.apiserver.exception.TokenRefreshException;
import com.smallnine.apiserver.logging.AuditLogger;
import com.smallnine.apiserver.logging.LogContext;
import com.smallnine.apiserver.service.MailService;
import com.smallnine.apiserver.service.RefreshTokenService;
import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.smallnine.apiserver.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogger auditLogger;
    private final MailService mailService;
    
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("action=register username={} email={} result=attempt", 
                request.getUsername(), request.getEmail());
        
        if (userDao.existsByUsername(request.getUsername())) {
            log.warn("action=register username={} result=failed reason=username_exists",
                    request.getUsername());
            throw new DuplicateResourceException("用戶", "username", request.getUsername());
        }

        if (userDao.existsByEmail(request.getEmail())) {
            log.warn("action=register email={} result=failed reason=email_exists",
                    request.getEmail());
            throw new DuplicateResourceException("用戶", "email", request.getEmail());
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealname(request.getRealname());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender() != null ? request.getGender() : User.Gender.male);
        user.setEmailValidated(false);

        // 生成郵件驗證令牌
        String verificationToken = generateSecureToken();
        user.setResetToken(verificationToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));

        userDao.insert(user);
        log.info("action=register username={} user_id={} result=success",
                user.getUsername(), user.getId());

        mailService.sendVerificationEmail(user.getEmail(), verificationToken);

        return new UserResponse(user);
    }
    
    public AuthResponse login(LoginRequest request) {
        log.debug("action=login user={} result=attempt", request.getUsernameOrEmail());

        User user = userDao.findByUsernameOrEmail(request.getUsernameOrEmail())
                .orElseThrow(() -> {
                    // 使用審計日誌記錄登入失敗
                    auditLogger.logLoginFailure(request.getUsernameOrEmail(), "用戶不存在");
                    return new UsernameNotFoundException("用戶名或密碼錯誤");
                });

        if (!user.isEnabled()) {
            auditLogger.logLoginFailure(user.getUsername(), "帳號已停用");
            throw new AccountDisabledException();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditLogger.logLoginFailure(user.getUsername(), "密碼錯誤");
            throw new BadCredentialsException("用戶名或密碼錯誤");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(user);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtUtil.getAccessTokenExpirationTime() / 1000);

        // 設置用戶上下文（後續日誌會自動帶上 userId）
        LogContext.setUser(user.getId().toString(), user.getUsername());

        // 記錄登入成功（安全審計日誌）
        auditLogger.logLoginSuccess(user.getId().toString(), user.getUsername());

        return new AuthResponse(accessToken, refreshTokenEntity.getToken(), user, expiresAt);
    }
    
    public UserResponse getCurrentUser(String username) {
        User user = userDao.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("找不到用戶: " + username));
        
        return new UserResponse(user);
    }
    
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserId)
                .map(userId -> {
                    User user = userDao.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("用戶", userId));
                    
                    String newAccessToken = jwtUtil.generateAccessToken(user.getUsername());
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);
                    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtUtil.getAccessTokenExpirationTime() / 1000);
                    
                    // 撤銷舊的更新令牌
                    refreshTokenService.revokeByToken(requestRefreshToken);
                    
                    log.info("action=refresh_token username={} user_id={} result=success", 
                            user.getUsername(), user.getId());
                    return new AuthResponse(newAccessToken, newRefreshToken.getToken(), user, expiresAt);
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "更新令牌無效或已過期"));
    }
    
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revokeByToken(refreshToken);
            log.info("action=logout result=success token_revoked=true");
        } else {
            log.warn("action=logout result=failed reason=missing_token");
        }
    }

    /**
     * 驗證郵件
     */
    @Transactional
    public void verifyEmail(String token) {
        log.info("action=verify_email token={} result=attempt", token.substring(0, Math.min(8, token.length())) + "...");

        User user = userDao.findByResetToken(token)
                .orElseThrow(() -> {
                    log.warn("action=verify_email result=failed reason=invalid_token");
                    return new ResourceNotFoundException("驗證令牌無效或已過期");
                });

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("action=verify_email user_id={} result=failed reason=token_expired", user.getId());
            throw new ResourceNotFoundException("驗證令牌已過期，請重新註冊");
        }

        user.setEmailValidated(true);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userDao.update(user);

        log.info("action=verify_email user_id={} username={} result=success", user.getId(), user.getUsername());
    }

    /**
     * 重新發送驗證郵件
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        log.info("action=resend_verification email={} result=attempt", email);

        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("用戶", email));

        if (user.getEmailValidated()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "該郵箱已驗證");
        }

        String verificationToken = generateSecureToken();
        user.setResetToken(verificationToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
        userDao.update(user);

        mailService.sendVerificationEmail(user.getEmail(), verificationToken);
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}