package com.smallnine.apiserver.dto;

import com.smallnine.apiserver.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用戶註冊請求")
public class RegisterRequest {
    
    @NotBlank(message = "用戶名不能為空")
    @Size(min = 3, max = 20, message = "用戶名長度必須在3-20字符之間")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用戶名只能包含字母、數字和下劃線")
    @Schema(description = "用戶名", example = "john123", required = true)
    private String username;
    
    @NotBlank(message = "信箱不能為空")
    @Email(message = "信箱格式不正確")
    @Schema(description = "信箱地址", example = "john@example.com", required = true)
    private String email;
    
    @NotBlank(message = "密碼不能為空")
    @Size(min = 6, max = 100, message = "密碼長度必須在6-100字符之間")
    @Schema(description = "密碼", example = "password123", required = true)
    private String password;
    
    @Schema(description = "真實姓名", example = "John Doe")
    private String realname;
    
    @Pattern(regexp = "^[0-9+\\-\\s()]*$", message = "手機號碼格式不正確")
    @Schema(description = "手機號碼", example = "0912345678")
    private String phone;
    
    @Schema(description = "性別", example = "male", allowableValues = {"male", "female"})
    private User.Gender gender = User.Gender.male;
}