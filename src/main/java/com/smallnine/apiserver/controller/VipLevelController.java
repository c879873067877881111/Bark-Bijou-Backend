package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.entity.VipLevel;
import com.smallnine.apiserver.service.impl.VipLevelServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vip-levels")
@RequiredArgsConstructor
@Tag(name = "VIP等級管理", description = "VIP會員等級管理API")
@SecurityRequirement(name = "bearerAuth")
public class VipLevelController {

    private final VipLevelServiceImpl vipLevelService;

    @GetMapping("/{id}")
    @Operation(summary = "根據ID查詢VIP等級", description = "通過VIP等級ID獲取詳細訊息")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VIP等級不存在")
    })
    public ApiResponse<VipLevel> getVipLevelById(
            @Parameter(description = "VIP等級ID", required = true)
            @PathVariable Long id) {
        VipLevel vipLevel = vipLevelService.findById(id);
        return ApiResponse.success(vipLevel);
    }

    @GetMapping
    @Operation(summary = "查詢所有VIP等級", description = "獲取所有VIP等級列表")
    public ApiResponse<List<VipLevel>> getAllVipLevels() {
        List<VipLevel> vipLevels = vipLevelService.findAll();
        return ApiResponse.success(vipLevels);
    }

    @GetMapping("/active")
    @Operation(summary = "查詢啟用的VIP等級", description = "獲取所有啟用狀態的VIP等級")
    public ApiResponse<List<VipLevel>> getActiveVipLevels() {
        List<VipLevel> vipLevels = vipLevelService.findAllActive();
        return ApiResponse.success(vipLevels);
    }

    @GetMapping("/by-spending")
    @Operation(summary = "根據消費金額獲取VIP等級", description = "根據用戶消費金額獲取匹配的VIP等級")
    public ApiResponse<VipLevel> getVipLevelBySpending(
            @Parameter(description = "消費金額", required = true, example = "1000.00")
            @RequestParam BigDecimal spending) {
        VipLevel vipLevel = vipLevelService.getVipLevelBySpending(spending);
        return ApiResponse.success(vipLevel);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "根據名稱查詢VIP等級", description = "通過VIP等級名稱查詢")
    public ApiResponse<VipLevel> getVipLevelByName(
            @Parameter(description = "VIP等級名稱", required = true)
            @PathVariable String name) {
        VipLevel vipLevel = vipLevelService.findByName(name);
        return ApiResponse.success(vipLevel);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "創建VIP等級", description = "創建新的VIP等級")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "創建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "VIP等級名稱已存在")
    })
    public ApiResponse<VipLevel> createVipLevel(
            @Parameter(description = "VIP等級訊息", required = true)
            @Valid @RequestBody VipLevel vipLevel) {
        VipLevel createdVipLevel = vipLevelService.createVipLevel(vipLevel);
        return ApiResponse.success(createdVipLevel, ResponseCode.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新VIP等級", description = "更新指定ID的VIP等級")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VIP等級不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "VIP等級名稱已存在")
    })
    public ApiResponse<VipLevel> updateVipLevel(
            @Parameter(description = "VIP等級ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "更新的VIP等級訊息", required = true)
            @Valid @RequestBody VipLevel vipLevel) {
        vipLevel.setId(id);
        VipLevel updatedVipLevel = vipLevelService.updateVipLevel(vipLevel);
        return ApiResponse.success(updatedVipLevel);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "刪除VIP等級", description = "刪除指定ID的VIP等級")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "刪除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VIP等級不存在")
    })
    public ApiResponse<Void> deleteVipLevel(
            @Parameter(description = "VIP等級ID", required = true)
            @PathVariable Long id) {
        vipLevelService.deleteVipLevel(id);
        return ApiResponse.success();
    }

    @GetMapping("/stats")
    @Operation(summary = "獲取VIP等級統計", description = "獲取VIP等級總數和啟用數量")
    public ApiResponse<Object> getVipLevelStats() {
        long total = vipLevelService.count();
        long active = vipLevelService.countActive();

        return ApiResponse.success(new Object() {
            public final long totalVipLevels = total;
            public final long activeVipLevels = active;
        });
    }
}
