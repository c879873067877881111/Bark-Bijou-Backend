package com.smallnine.apiserver.service;

import java.util.Map;

public interface OtpService {
    /** Returns map with keys "otp" and "secret" */
    Map<String, String> generateAndSend(String email);
    boolean verify(String email, String otp);
    void cleanup(String email);
    void cleanupBySecret(String secret);
}
