package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.dto.NotificationRequest;
import com.smallnine.apiserver.entity.Notification;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.NotificationService;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "通知", description = "通知管理 API")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "取得我的通知")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getByMemberId(user.getId())));
    }

    @Operation(summary = "標記單筆已讀")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/read/{id}")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("已標記已讀"));
    }

    @Operation(summary = "新增通知")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponse<Notification>> createNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NotificationRequest request) {
        if (userDetails == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("未登入"));
        }
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Notification notification = new Notification();
        notification.setMemberId(user.getId());
        notification.setMessage(request.getMessage());
        notification.setIsRead(false);
        notificationService.create(notification);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @Operation(summary = "全部已讀")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.success("已全部標記已讀"));
    }

    @Operation(summary = "刪除通知")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        notificationService.delete(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("刪除成功"));
    }
}
