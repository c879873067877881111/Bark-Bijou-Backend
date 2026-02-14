package com.smallnine.apiserver.service;

public interface MailService {
    void sendVerificationEmail(String to, String token);
    void sendOtpEmail(String to, String otp);
    void sendResetPasswordEmail(String to, String otp);
}
