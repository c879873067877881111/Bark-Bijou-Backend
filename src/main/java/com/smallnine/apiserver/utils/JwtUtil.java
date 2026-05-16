package com.smallnine.apiserver.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.access-token.expiration}")
    private Long accessTokenExpiration;
    
    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenExpiration;
    
    // 簽章金鑰只在 startup 建一次：驗證與實際使用走同一條路徑，
    // 不再「平時沒事、第一次簽 token 才炸」。
    private SecretKey signingKey;

    /**
     * startup 即驗證 jwt.secret：缺值/空白/不足 256 bit 直接讓 context 啟動失敗，
     * 把失敗點從「第一次簽 token」提前到「服務起不來」。
     * getBytes 鎖死 UTF-8，不吃 platform default charset（跨環境結果一致）。
     */
    @PostConstruct
    void initSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret 未設定：請在 .env 設定 JWT_SECRET（HS256 需至少 32 bytes）");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret 太短：HS256 需至少 32 bytes（256 bit），目前僅 "
                            + keyBytes.length + " bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("存取令牌已過期: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("不支援的令牌格式: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("令牌格式錯誤: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.warn("令牌簽名驗證失敗: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("令牌參數無效: {}", e.getMessage());
            throw e;
        }
    }
    
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }
    
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }
    
    public String generateAccessToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        return createToken(claims, username, accessTokenExpiration);
    }
    
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>(); 
        claims.put("type", "refresh");
        return createToken(claims, username, refreshTokenExpiration);
    }
    
    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, accessTokenExpiration);
    }
    
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }
    
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> (String) claims.get("type"));
    }
    
    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }
    
    public boolean isAccessToken(String token) {
        return "access".equals(extractTokenType(token));
    }
    
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("令牌驗證失敗: {}", e.getMessage());
            return false;
        }
    }
    
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("令牌驗證失敗: {}", e.getMessage());
            return false;
        }
    }
    
    public Long getAccessTokenExpirationTime() {
        return accessTokenExpiration;
    }
    
    public Long getRefreshTokenExpirationTime() {
        return refreshTokenExpiration;
    }
}