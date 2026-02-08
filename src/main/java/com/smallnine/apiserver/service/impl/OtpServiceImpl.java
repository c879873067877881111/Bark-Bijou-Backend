package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.EmailVerificationDao;
import com.smallnine.apiserver.entity.EmailVerification;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;

    private final EmailVerificationDao emailVerificationDao;

    @Override
    @Transactional
    public Map<String, String> generateAndSend(String email) {
        // Remove old OTPs for this email
        emailVerificationDao.deleteByEmail(email);

        String otp = generateOtp();
        String secret = UUID.randomUUID().toString();

        EmailVerification verification = new EmailVerification();
        verification.setEmail(email);
        verification.setOtpToken(otp);
        verification.setSecret(secret);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));

        emailVerificationDao.insert(verification);
        log.info("action=generate_otp email={} result=success", email);
        return Map.of("otp", otp, "secret", secret);
    }

    @Override
    public boolean verify(String email, String otp) {
        EmailVerification verification = emailVerificationDao.findByEmailAndOtp(email, otp)
                .orElseThrow(() -> new BusinessException(ResponseCode.OTP_INVALID));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResponseCode.OTP_EXPIRED);
        }
        return true;
    }

    @Override
    @Transactional
    public void cleanup(String email) {
        emailVerificationDao.deleteByEmail(email);
    }

    @Override
    @Transactional
    public void cleanupBySecret(String secret) {
        emailVerificationDao.findBySecret(secret)
                .ifPresent(v -> emailVerificationDao.deleteByEmail(v.getEmail()));
    }

    private String generateOtp() {
        int num = RANDOM.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", num);
    }
}
