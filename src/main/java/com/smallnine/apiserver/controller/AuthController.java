package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dto.*;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.security.oauth2.OAuth2ExchangeService;
import com.smallnine.apiserver.service.AuthService;
import com.smallnine.apiserver.service.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "用戶認證", description = "用戶登入、註冊和認證相關 API")
public class AuthController {
    
    private final AuthService authService;
    private final OAuth2ExchangeService oAuth2ExchangeService;
    private final RateLimiterService rateLimiterService;

    // #H1 fast-follow：只有部署在可信反向代理後才採信 X-Forwarded-For，
    // 否則任何 client 都能偽造此 header，per-IP 限流形同虛設。預設關閉。
    @Value("${app.rate-limit.trust-forwarded-for:false}")
    private boolean trustForwardedFor;
    
    @Operation(summary = "用戶註冊", description = "註冊新用戶帳號")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "註冊成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數無效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "用戶名或信箱已存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "內部服務器錯誤")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("註冊成功", user));
    }
    
    @Operation(summary = "用戶登入", description = "用戶登入並獲取存取令牌")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登入成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用戶名或密碼錯誤"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "帳號已被停用"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "內部服務器錯誤")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("登入成功", authResponse));
    }
    
    @Operation(summary = "獲取當前用戶訊息", description = "根據存取令牌獲取當前登入用戶的詳細訊息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功獲取用戶訊息"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用戶不存在")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("未授權訪問"));
        }
        UserResponse user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    @Operation(summary = "更新存取令牌", description = "使用更新令牌重新獲取存取令牌")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "刷新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "更新令牌無效或已過期"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "更新令牌不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "內部服務器錯誤")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("存取令牌更新成功", authResponse));
    }
    
    @Operation(summary = "用戶登出", description = "撤銷更新令牌並登出用戶")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登出成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "內部服務器錯誤")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("登出成功"));
    }

    @Operation(summary = "驗證郵箱", description = "通過驗證令牌驗證用戶郵箱")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "郵箱驗證成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "驗證令牌無效或已過期")
    })
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("郵箱驗證成功，現在可以登入"));
    }

    @Operation(summary = "OAuth2 一次性 code 換 token",
            description = "Google 登入成功後，前端從 redirect URL 取得 code，再呼叫此端點換取 access/refresh token。code 為一次性，60 秒內有效。")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "兌換成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "code 無效或已過期")
    })
    @PostMapping("/oauth/exchange")
    public ResponseEntity<ApiResponse<AuthResponse>> exchangeOAuth2Code(@Valid @RequestBody OAuth2ExchangeRequest request) {
        AuthResponse authResponse = oAuth2ExchangeService.exchange(request.getCode());
        return ResponseEntity.ok(ApiResponse.success("登入成功", authResponse));
    }

    @Operation(summary = "重新發送驗證郵件", description = "重新發送郵箱驗證郵件；為防帳號枚舉，無論信箱是否存在皆回固定訊息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "已受理（不洩漏信箱是否存在）"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "請求過於頻繁")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerification(@RequestParam String email,
                                                                  HttpServletRequest request) {
        // #H1 限流：email 與 IP 任一超限即擋，避免 email 炸彈 / 整段 IP 掃信箱
        if (!rateLimiterService.tryResendVerification(email, clientIp(request))) {
            throw new BusinessException(ResponseCode.TOO_MANY_REQUESTS);
        }
        // #H1 防枚舉：service 對「不存在 / 已驗證」靜默處理，這裡一律回固定訊息
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success("若該信箱已註冊且尚未驗證，我們已寄出驗證信"));
    }

    /**
     * 取用戶端 IP。X-Forwarded-For 可被 client 任意偽造，只有確定部署在
     * 可信反向代理後（app.rate-limit.trust-forwarded-for=true）才採信其第一段；
     * 否則一律用 remoteAddr，避免偽造 header 繞過 per-IP 限流。
     */
    private String clientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}