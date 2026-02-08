package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.dto.GoogleLoginRequest;
import com.smallnine.apiserver.entity.RefreshToken;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.RefreshTokenService;
import com.smallnine.apiserver.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/member/login")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Google 登入", description = "Google OAuth 登入 API")
public class GoogleLoginController {

    private final UserDao userDao;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Google 登入")
    @PostMapping("/google-login")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody GoogleLoginRequest request) {
        boolean isNew = false;

        // Find or create user by Google UID
        User user = userDao.findByGoogleUid(request.getUid())
                .orElseGet(() -> userDao.findByEmail(request.getEmail()).orElse(null));

        if (user == null) {
            // Create new user
            isNew = true;
            user = new User();
            user.setUsername("google_" + request.getUid().substring(0, 8));
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRealname(request.getDisplayName() != null ? request.getDisplayName() : "Google 用戶");
            user.setGoogleUid(request.getUid());
            user.setGoogleName(request.getDisplayName());
            user.setImageUrl(request.getPhotoURL() != null ? request.getPhotoURL() : "/member/member_images/user-img.svg");
            user.setEmailValidated(true);
            user.setGender(User.Gender.male);
            userDao.insert(user);
            log.info("action=google_login new_user={} email={}", user.getUsername(), user.getEmail());
        } else if (user.getGoogleUid() == null) {
            // Link Google account to existing user
            user.setGoogleUid(request.getUid());
            user.setGoogleName(request.getDisplayName());
            if (request.getPhotoURL() != null) {
                user.setImageUrl(request.getPhotoURL());
            }
            user.setEmailValidated(true);
            userDao.update(user);
            log.info("action=google_login linked_user={} email={}", user.getUsername(), user.getEmail());
        }

        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtUtil.getAccessTokenExpirationTime() / 1000);

        Map<String, Object> authData = new LinkedHashMap<>();
        authData.put("accessToken", accessToken);
        authData.put("refreshToken", refreshToken.getToken());
        authData.put("id", user.getId());
        authData.put("username", user.getUsername());
        authData.put("email", user.getEmail());
        authData.put("expiresAt", expiresAt.toString());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        body.put("message", "登入成功");
        body.put("data", authData);
        body.put("isNew", isNew);
        body.put("id", user.getId());
        return ResponseEntity.ok(body);
    }
}
