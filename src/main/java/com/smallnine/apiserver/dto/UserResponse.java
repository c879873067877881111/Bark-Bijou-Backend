package com.smallnine.apiserver.dto;

import com.smallnine.apiserver.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用戶訊息回應")
public class UserResponse {
    
    @Schema(description = "用戶ID", example = "1")
    private Long id;
    
    @Schema(description = "用戶名", example = "john123")
    private String username;
    
    @Schema(description = "信箱", example = "john@example.com")
    private String email;
    
    @Schema(description = "全名", example = "John Doe")
    private String fullName;
    
    @Schema(description = "手機號碼", example = "+886-912345678")
    private String phoneNumber;
    
    @Schema(description = "性別", example = "male")
    private User.Gender gender;
    
    @Schema(description = "會員等級ID", example = "1")
    private Integer vipLevelsId;
    
    @Schema(description = "頭像URL", example = "/member/member_images/user-img.svg")
    private String imageUrl;
    
    @Schema(description = "信箱是否已驗證", example = "false")
    private Boolean emailValidated;
    
    @Schema(description = "創建時間")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新時間")
    private LocalDateTime updatedAt;
    
    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullName = user.getRealname();
        this.phoneNumber = user.getPhone();
        this.gender = user.getGender();
        this.vipLevelsId = user.getVipLevelsId();
        this.imageUrl = user.getImageUrl();
        this.emailValidated = user.getEmailValidated();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}