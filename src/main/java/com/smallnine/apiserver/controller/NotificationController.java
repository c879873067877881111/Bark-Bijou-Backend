package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.entity.Notification;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.NotificationService;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "通知", description = "通知管理 API")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "取得我的通知")
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(notificationService.getByMemberId(user.getId()));
    }

    @Operation(summary = "標記單筆已讀")
    @PostMapping("/read/{id}")
    public ResponseEntity<Map<String, String>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(Map.of("message", "已標記已讀"));
    }

    @Operation(summary = "新增通知")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        if (userDetails == null) {
            return ResponseEntity.badRequest().body(Map.of("message", (Object) "未登入"));
        }
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Notification notification = new Notification();
        notification.setMemberId(user.getId());
        notification.setMessage(body.get("message"));
        notification.setIsRead(false);
        notificationService.create(notification);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", notification.getId());
        result.put("content", notification.getMessage());
        result.put("created_at", notification.getCreatedAt());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "全部已讀")
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("message", "已全部標記已讀"));
    }

    @Operation(summary = "刪除通知")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        notificationService.delete(id, user.getId());
        return ResponseEntity.ok(Map.of("message", "刪除成功"));
    }
}
