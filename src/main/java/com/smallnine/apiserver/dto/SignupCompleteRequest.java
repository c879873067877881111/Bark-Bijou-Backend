package com.smallnine.apiserver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupCompleteRequest {
    @NotBlank(message = "用戶名不能為空")
    private String username;

    @NotBlank(message = "信箱不能為空")
    @Email
    private String email;

    @NotBlank(message = "密碼不能為空")
    @Size(min = 6, message = "密碼長度至少6位")
    private String password;

    @NotBlank(message = "確認密碼不能為空")
    private String repassword;
}
