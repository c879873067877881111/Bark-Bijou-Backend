package com.smallnine.apiserver.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;

@Data
public class UserUpdateDTO {
    
    @Size(max = 50, message = "真實姓名長度不能超過50字符")
    private String fullName;
    
    @Size(max = 15, message = "電話號碼長度不能超過15字符")
    private String phoneNumber;
    
    private String gender;
    
    private String imageUrl;
}