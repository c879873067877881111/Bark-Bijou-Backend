package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.MemberService;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "會員資料", description = "會員個人資料管理 API")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "更新會員資料 (FormData)")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/api/member/profile/edit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realname,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String birth_date,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) MultipartFile avatar) {

        User user = AuthUtils.getAuthenticatedUser(userDetails);
        String imageUrl = memberService.updateProfile(user, username, realname, email, birth_date, gender, phone, avatar);

        Map<String, Object> data = new LinkedHashMap<>();
        if (imageUrl != null) {
            data.put("image_url", imageUrl);
        }
        return ResponseEntity.ok(ApiResponse.success("更新成功", data));
    }

    @Operation(summary = "修改密碼 (URLSearchParams)")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/api/member/profile/{memberId}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long memberId,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {

        User authenticatedUser = AuthUtils.getAuthenticatedUser(userDetails);
        memberService.changePassword(memberId, authenticatedUser.getId(), currentPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success("密碼修改成功"));
    }
}
