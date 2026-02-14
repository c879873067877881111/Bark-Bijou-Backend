package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private Role role = Role.USER;
    private String username;
    private String realname;
    private String email;
    private String password;
    private String phone;
    private Gender gender;
    private LocalDate birthDate;
    private Integer vipLevelsId = 1;
    private String imageUrl = "/member/member_images/user-img.svg";
    private LocalDateTime createdAt;
    private Boolean emailValidated = false;
    private String googleUid;
    private LocalDateTime imageUpdatedAt;
    private String resetToken;
    private LocalDateTime resetTokenExpiry;
    private String resetTokenSecret;
    private String googleName;
    private String city;
    private String address;
    private String zip;
    private LocalDateTime updatedAt;


    public boolean isEnabled() {
        return emailValidated != null ? emailValidated : false;
    }

    public enum Gender {
        male, female
    }
    
    public enum Role {
        USER, ADMIN, SITTER
    }
}