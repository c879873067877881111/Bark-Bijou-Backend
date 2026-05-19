package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.EmailVerificationDao;
import com.smallnine.apiserver.dao.UserDao;
import com.smallnine.apiserver.entity.EmailVerification;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.exception.ResourceNotFoundException;
import com.smallnine.apiserver.service.ForgotPasswordService;
import com.smallnine.apiserver.service.MailService;
import com.smallnine.apiserver.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private final OtpService otpService;
    private final MailService mailService;
    private final UserDao userDao;
    private final EmailVerificationDao emailVerificationDao;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Map<String, String> sendResetOtp(String email) {
        userDao.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("用戶", email));

        Map<String, String> result = otpService.generateAndSend(email);
        mailService.sendResetPasswordEmail(email, result.get("otp"));

        return Map.of("secret", result.get("secret"));
    }

    @Override
    public void resetPasswordByOtp(String secret, String otpToken, String newPassword) {
        EmailVerification verification = emailVerificationDao.findBySecretAndOtp(secret, otpToken)
                .orElseThrow(() -> new BusinessException(ResponseCode.OTP_INVALID));

        doResetPassword(verification, newPassword);
    }

    @Override
    public void resetPasswordByLink(String secret, String newPassword) {
        EmailVerification verification = emailVerificationDao.findBySecret(secret)
                .orElseThrow(() -> new BusinessException(ResponseCode.OTP_INVALID, "連結無效或已過期"));

        doResetPassword(verification, newPassword);
    }

    private void doResetPassword(EmailVerification verification, String newPassword) {
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResponseCode.OTP_EXPIRED);
        }

        User user = userDao.findByEmail(verification.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("用戶", verification.getEmail()));

        userDao.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        otpService.cleanup(verification.getEmail());
    }
}