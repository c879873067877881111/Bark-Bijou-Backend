package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerification {
    private Long id;
    private String email;
    private String otpToken;
    private String secret;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
