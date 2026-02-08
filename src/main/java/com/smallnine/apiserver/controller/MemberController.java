package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "會員資料", description = "會員個人資料管理 API")
public class MemberController {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "更新會員資料 (FormData)")
    @PutMapping("/api/member/profile/edit")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String birth_date,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) MultipartFile avatar) {

        User user = AuthUtils.getAuthenticatedUser(userDetails);

        if (username != null) user.setUsername(username);
        if (email != null) user.setEmail(email);
        if (gender != null) {
            try { user.setGender(User.Gender.valueOf(gender)); } catch (Exception ignored) {}
        }
        if (phone != null) user.setPhone(phone);
        if (birth_date != null && !birth_date.isEmpty()) {
            user.setBirthDate(java.time.LocalDate.parse(birth_date));
        }

        if (avatar != null && !avatar.isEmpty()) {
            String originalFilename = avatar.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String safeFilename = UUID.randomUUID() + ext;
            String imageUrl = "/member/member_images/" + safeFilename;
            user.setImageUrl(imageUrl);
        }

        userDao.updateProfile(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "更新成功");
        if (user.getImageUrl() != null) {
            result.put("image_url", user.getImageUrl());
        }
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "修改密碼 (URLSearchParams)")
    @PutMapping("/api/member/profile/{memberId}/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long memberId,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {

        User authenticatedUser = AuthUtils.getAuthenticatedUser(userDetails);
        if (!authenticatedUser.getId().equals(memberId)) {
            return ResponseEntity.status(403).body(Map.of("message", "無權限修改他人密碼"));
        }

        if (!passwordEncoder.matches(currentPassword, authenticatedUser.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "舊密碼錯誤"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "新密碼長度至少6位"));
        }

        userDao.updatePassword(memberId, passwordEncoder.encode(newPassword));
        return ResponseEntity.ok(Map.of("message", "密碼修改成功"));
    }
}
