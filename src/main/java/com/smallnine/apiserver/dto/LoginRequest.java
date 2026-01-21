package com.smallnine.apiserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用戶登入請求")
public class LoginRequest {
    
    @NotBlank(message = "用戶名或信箱不能為空")
    @Schema(description = "用戶名或信箱", example = "john123", required = true)
    private String usernameOrEmail;
    
    @NotBlank(message = "密碼不能為空")
    @Schema(description = "密碼", example = "password123", required = true)
    private String password;
}