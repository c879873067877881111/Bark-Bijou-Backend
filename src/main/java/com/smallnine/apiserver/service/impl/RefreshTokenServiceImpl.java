package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.dao.RefreshTokenDao;
import com.smallnine.apiserver.entity.RefreshToken;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.TokenRefreshException;
import com.smallnine.apiserver.service.RefreshTokenService;
import com.smallnine.apiserver.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {
    
    private final RefreshTokenDao refreshTokenDao;
    private final JwtUtil jwtUtil;
    
    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // 先撤銷該用戶現有的 refresh token
        revokeByUser(user);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpirationTime() / 1000));
        refreshToken.setRevoked(false);
        
        refreshTokenDao.save(refreshToken);
        log.info("為用戶 {} 創建新的 refresh token", user.getUsername());
        
        return refreshToken;
    }
    
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenDao.findByToken(token);
    }
    
    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenDao.revokeByToken(token.getToken());
            throw new TokenRefreshException(token.getToken(), "Refresh token 已過期,請重新登入");
        }
        return token;
    }
    
    @Override
    @Transactional
    public void revokeByUser(User user) {
        refreshTokenDao.revokeByUserId(user.getId());
        log.info("撤銷用戶 {} 的所有 refresh token", user.getUsername());
    }
    
    @Override
    @Transactional
    public void revokeByToken(String token) {
        refreshTokenDao.revokeByToken(token);
        log.info("撤銷 refresh token: {}", token.substring(0, 8) + "...");
    }
    
    @Override
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenDao.deleteExpiredTokens(LocalDateTime.now());
        log.info("清理過期的 refresh tokens");
    }
}