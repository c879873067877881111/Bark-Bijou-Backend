package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.PointsService;
import com.smallnine.apiserver.utils.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/me/points")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "會員積分", description = "會員積分查詢 API")
public class PointsController {

    private final PointsService pointsService;

    @Operation(summary = "取得積分摘要", description = "取得當前會員的積分總計與歷史紀錄")
    @GetMapping
    public ApiResponse<Map<String, Object>> getPointsSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        return ApiResponse.success(pointsService.getPointsSummary(user.getId()));
    }
}
