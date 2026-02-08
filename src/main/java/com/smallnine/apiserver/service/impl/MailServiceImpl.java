package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailServiceImpl implements MailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String to, String otp) {
        if (mailSender == null) {
            log.warn("[DEV] OTP for {}: {}", to, otp);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("BARK & BIJOU 註冊驗證碼");
            message.setText("您的驗證碼為: " + otp + "\n\n驗證碼將在 10 分鐘後失效。\n如非本人操作請忽略此郵件。");
            mailSender.send(message);
            log.info("action=send_otp_email to={} result=success", to);
        } catch (Exception e) {
            log.error("action=send_otp_email to={} result=failed error={}", to, e.getMessage());
            log.warn("[DEV] OTP for {}: {}", to, otp);
        }
    }

    @Override
    public void sendResetPasswordEmail(String to, String otp) {
        if (mailSender == null) {
            log.warn("[DEV] Reset OTP for {}: {}", to, otp);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("BARK & BIJOU 重設密碼驗證碼");
            message.setText("您的重設密碼驗證碼為: " + otp + "\n\n驗證碼將在 10 分鐘後失效。\n如非本人操作請忽略此郵件。");
            mailSender.send(message);
            log.info("action=send_reset_email to={} result=success", to);
        } catch (Exception e) {
            log.error("action=send_reset_email to={} result=failed error={}", to, e.getMessage());
            log.warn("[DEV] Reset OTP for {}: {}", to, otp);
        }
    }
}
