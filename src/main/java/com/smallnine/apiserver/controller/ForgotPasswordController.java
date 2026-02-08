package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dao.EmailVerificationDao;
import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.dto.SendOtpRequest;
import com.smallnine.apiserver.entity.EmailVerification;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.service.MailService;
import com.smallnine.apiserver.service.OtpService;
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
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "忘記密碼", description = "忘記密碼 OTP 流程 API")
public class ForgotPasswordController {

    private final OtpService otpService;
    private final MailService mailService;
    private final UserDao userDao;
    private final EmailVerificationDao emailVerificationDao;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "發送重設密碼 OTP")
    @PostMapping("/api/member/requestotp")
    public ResponseEntity<Map<String, Object>> requestOtp(@Valid @RequestBody SendOtpRequest request) {
        User user = userDao.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "此信箱尚未註冊"));
        }

        Map<String, String> result = otpService.generateAndSend(request.getEmail());
        mailService.sendResetPasswordEmail(request.getEmail(), result.get("otp"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("secret", result.get("secret"));
        body.put("message", "驗證碼已寄出，請至信箱查收");
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "重設密碼 (透過 OTP)")
    @PostMapping("/api/member/resetpassword")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        String secret = request.get("secret");
        String otpToken = request.get("otpToken");
        String newPassword = request.get("newPassword");

        if (secret == null || otpToken == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "缺少必要欄位"));
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "密碼長度至少6位"));
        }

        // Find verification record by secret + otp
        EmailVerification verification = emailVerificationDao.findBySecretAndOtp(secret, otpToken)
                .orElse(null);
        if (verification == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "驗證碼錯誤"));
        }
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "驗證碼已過期"));
        }

        User user = userDao.findByEmail(verification.getEmail())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "用戶不存在"));
        }

        userDao.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        otpService.cleanup(verification.getEmail());

        return ResponseEntity.ok(Map.of("message", "密碼重設成功"));
    }

    @Operation(summary = "重設密碼 (透過連結)")
    @PostMapping("/api/member/resetpassword/bylink")
    public ResponseEntity<Map<String, Object>> resetPasswordByLink(@RequestBody Map<String, String> request) {
        String secret = request.get("secret");
        String newPassword = request.get("newPassword");

        if (secret == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "缺少必要欄位"));
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "密碼長度至少6位"));
        }

        // Find verification record by secret
        EmailVerification verification = emailVerificationDao.findBySecret(secret)
                .orElse(null);
        if (verification == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "連結無效或已過期"));
        }
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "連結已過期"));
        }

        User user = userDao.findByEmail(verification.getEmail())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "用戶不存在"));
        }

        userDao.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        otpService.cleanup(verification.getEmail());

        return ResponseEntity.ok(Map.of("message", "密碼重設成功"));
    }
}
