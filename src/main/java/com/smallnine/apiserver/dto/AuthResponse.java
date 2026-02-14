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
@Schema(description = "認證回應")
public class AuthResponse {
    
    @Schema(description = "存取令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;
    
    @Schema(description = "更新令牌", example = "abc123-def456-ghi789...")
    private String refreshToken;
    
    @Schema(description = "令牌類型", example = "Bearer")
    private String type = "Bearer";
    
    @Schema(description = "用戶ID", example = "1")
    private Long id;
    
    @Schema(description = "用戶名", example = "john123")
    private String username;
    
    @Schema(description = "信箱", example = "john@example.com")
    private String email;
    
    @Schema(description = "真實姓名", example = "王小明")
    private String realname;
    
    @Schema(description = "性別", example = "male")
    private User.Gender gender;
    
    @Schema(description = "存取令牌過期時間")
    private LocalDateTime expiresAt;
    
    public AuthResponse(String accessToken, String refreshToken, User user, LocalDateTime expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.realname = user.getRealname();
        this.gender = user.getGender();
        this.expiresAt = expiresAt;
    }
}