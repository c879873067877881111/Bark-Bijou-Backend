package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.dto.*;
import com.smallnine.apiserver.entity.RefreshToken;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.AccountDisabledException;
import com.smallnine.apiserver.exception.DuplicateResourceException;
import com.smallnine.apiserver.exception.ResourceNotFoundException;
import com.smallnine.apiserver.logging.AuditLogger;
import com.smallnine.apiserver.logging.LogContext;
import com.smallnine.apiserver.service.RefreshTokenService;
import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogger auditLogger;
    
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
        user.setEmailValidated(true); // 暫時自動驗證,生產環境應該通過郵件驗證
        
        userDao.insert(user);
        log.info("action=register username={} user_id={} result=success", 
                user.getUsername(), user.getId());
        
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
                .orElseThrow(() -> new RuntimeException("更新令牌無效"));
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
}