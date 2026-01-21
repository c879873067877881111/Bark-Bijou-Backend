package com.smallnine.apiserver.dao;

import com.smallnine.apiserver.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface RefreshTokenDao {
    
    void save(RefreshToken refreshToken);
    
    Optional<RefreshToken> findByToken(@Param("token") String token);
    
    Optional<RefreshToken> findByUserId(@Param("userId") Long userId);
    
    void revokeByUserId(@Param("userId") Long userId);
    
    void revokeByToken(@Param("token") String token);
    
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    List<RefreshToken> findExpiredTokens(@Param("now") LocalDateTime now);
}