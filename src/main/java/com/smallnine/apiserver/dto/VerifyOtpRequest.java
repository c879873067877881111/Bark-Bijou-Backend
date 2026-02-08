package com.smallnine.apiserver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank(message = "信箱不能為空")
    @Email(message = "信箱格式不正確")
    private String email;

    @NotBlank(message = "驗證碼不能為空")
    private String otp;

    private String secret;
}
