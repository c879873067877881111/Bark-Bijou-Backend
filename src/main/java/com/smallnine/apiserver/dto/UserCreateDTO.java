package com.smallnine.apiserver.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class UserCreateDTO {
    
    @NotBlank(message = "用戶名不能為空")
    @Size(min = 3, max = 20, message = "用戶名長度必須在3-20字符之間")
    private String username;
    
    @NotBlank(message = "信箱不能為空")
    @Email(message = "信箱格式不正確")
    private String email;
    
    @NotBlank(message = "密碼不能為空")
    @Size(min = 6, max = 50, message = "密碼長度必須在6-50字符之間")
    private String password;
    
    @Size(max = 50, message = "真實姓名長度不能超過50字符")
    private String fullName;
    
    @Size(max = 15, message = "電話號碼長度不能超過15字符")
    private String phoneNumber;
    
    private String gender;
}