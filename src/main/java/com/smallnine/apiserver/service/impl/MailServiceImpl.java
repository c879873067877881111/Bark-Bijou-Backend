package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailServiceImpl implements MailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrls;

    @Override
    public void sendVerificationEmail(String to, String token) {
        String frontendUrl = frontendUrls.split(",")[0].trim();
        String verifyLink = frontendUrl + "/verify-email?token=" + token;

        String subject = "Bark Bijou - Email Verification";
        String htmlContent = """
                <div style="max-width:600px;margin:0 auto;font-family:Arial,sans-serif;padding:20px">
                  <h2 style="color:#333">Email Verification</h2>
                  <p>Thank you for registering. Please click the button below to verify your email:</p>
                  <a href="%s"
                     style="display:inline-block;padding:12px 24px;background:#4CAF50;color:#fff;
                            text-decoration:none;border-radius:4px;margin:16px 0">
                    Verify Email
                  </a>
                  <p style="color:#666;font-size:14px">
                    If the button doesn't work, copy and paste this link into your browser:<br/>
                    <a href="%s">%s</a>
                  </p>
                  <p style="color:#999;font-size:12px">This link expires in 24 hours.</p>
                </div>
                """.formatted(verifyLink, verifyLink, verifyLink);

        sendHtmlEmail(to, subject, htmlContent);
        log.info("action=send_verification_email to={} result=success", to);
    }

    @Override
    public void sendOtpEmail(String to, String otp) {
        String subject = "Bark Bijou - Your Verification Code";
        String htmlContent = """
                <div style="max-width:600px;margin:0 auto;font-family:Arial,sans-serif;padding:20px">
                  <h2 style="color:#333">Verification Code</h2>
                  <p>Your verification code is:</p>
                  <p style="font-size:32px;font-weight:bold;color:#4CAF50;letter-spacing:4px">%s</p>
                  <p style="color:#999;font-size:12px">This code expires in 10 minutes.</p>
                </div>
                """.formatted(otp);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendResetPasswordEmail(String to, String otp) {
        String subject = "Bark Bijou - Password Reset";
        String htmlContent = """
                <div style="max-width:600px;margin:0 auto;font-family:Arial,sans-serif;padding:20px">
                  <h2 style="color:#333">Password Reset</h2>
                  <p>Your password reset code is:</p>
                  <p style="font-size:32px;font-weight:bold;color:#FF5722;letter-spacing:4px">%s</p>
                  <p style="color:#999;font-size:12px">This code expires in 10 minutes.</p>
                </div>
                """.formatted(otp);

        sendHtmlEmail(to, subject, htmlContent);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (mailSender == null) {
            log.warn("[DEV] Mail not configured. subject='{}' to={}", subject, to);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("action=send_email to={} result=failed reason={}", to, e.getMessage());
            throw new BusinessException(ResponseCode.MAIL_SEND_FAILED, "郵件發送失敗，請稍後再試");
        }
    }
}
