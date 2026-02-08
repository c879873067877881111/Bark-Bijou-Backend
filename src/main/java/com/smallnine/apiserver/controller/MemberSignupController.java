package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dao.EmailVerificationDao;
import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.dto.*;
import com.smallnine.apiserver.entity.RefreshToken;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.MailService;
import com.smallnine.apiserver.service.OtpService;
import com.smallnine.apiserver.service.RefreshTokenService;
import com.smallnine.apiserver.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member/signup")
@RequiredArgsConstructor
@Tag(name = "會員註冊", description = "OTP 註冊流程 API")
public class MemberSignupController {

    private final OtpService otpService;
    private final MailService mailService;
    private final UserDao userDao;
    private final EmailVerificationDao emailVerificationDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "發送 OTP 驗證碼")
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        if (userDao.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "error", "message", "此信箱已被註冊"));
        }
        Map<String, String> result = otpService.generateAndSend(request.getEmail());
        mailService.sendOtpEmail(request.getEmail(), result.get("otp"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        body.put("message", "驗證碼已寄出，請至信箱查收");
        body.put("secret", result.get("secret"));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "驗證 OTP")
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            otpService.verify(request.getEmail(), request.getOtp());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "success");
            body.put("message", "信箱驗證成功");
            return ResponseEntity.ok(body);
        } catch (BusinessException e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "error");
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
    }

    @Operation(summary = "完成註冊")
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> complete(@Valid @RequestBody SignupCompleteRequest request) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getRepassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "兩次密碼輸入不一致"));
        }

        // Check email was OTP-verified (record still exists in DB)
        if (!emailVerificationDao.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "請先完成信箱驗證"));
        }

        if (userDao.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "error", "message", "用戶名已被使用",
                            "errors", List.of(Map.of("field", "username", "reason", "用戶名已被使用"))));
        }
        if (userDao.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "error", "message", "此信箱已被註冊",
                            "errors", List.of(Map.of("field", "email", "reason", "此信箱已被註冊"))));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealname(request.getUsername());
        user.setGender(User.Gender.male);
        user.setEmailValidated(true);

        userDao.insert(user);
        otpService.cleanup(request.getEmail());

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
        body.put("message", "註冊成功");
        body.put("data", authData);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "欄位即時驗證")
    @PostMapping("/validate-field")
    public ResponseEntity<Map<String, Object>> validateField(@RequestBody Map<String, String> request) {
        String field = request.get("field");
        String value = request.get("value");

        if (field == null || value == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "缺少 field 或 value"));
        }

        switch (field) {
            case "email":
                if (userDao.existsByEmail(value)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "此信箱已被註冊",
                                    "errors", List.of(Map.of("field", "email", "reason", "此信箱已被註冊"))));
                }
                break;
            case "username":
                if (userDao.existsByUsername(value)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "用戶名已被使用",
                                    "errors", List.of(Map.of("field", "username", "reason", "用戶名已被使用"))));
                }
                break;
            case "password":
                if (value.length() < 6) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "密碼長度至少6位",
                                    "errors", List.of(Map.of("field", "password", "reason", "密碼長度至少6位"))));
                }
                break;
            default:
                break;
        }

        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
