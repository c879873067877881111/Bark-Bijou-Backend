package com.smallnine.apiserver.service;

import com.smallnine.apiserver.entity.RefreshToken;
import com.smallnine.apiserver.entity.User;

import java.util.Optional;

public interface RefreshTokenService {
    
    RefreshToken createRefreshToken(User user);
    
    Optional<RefreshToken> findByToken(String token);
    
    RefreshToken verifyExpiration(RefreshToken token);
    
    void revokeByUser(User user);
    
    void revokeByToken(String token);
    
    void deleteExpiredTokens();
}