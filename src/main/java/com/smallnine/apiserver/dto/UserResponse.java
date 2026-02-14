package com.smallnine.apiserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smallnine.apiserver.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    @Schema(description = "真實姓名", example = "王小明")
    private String realname;

    @Schema(description = "手機號碼", example = "0912345678")
    private String phone;

    @Schema(description = "性別", example = "male")
    private User.Gender gender;

    @Schema(description = "生日")
    @JsonProperty("birth_date")
    private LocalDate birthDate;

    @Schema(description = "會員等級ID", example = "1")
    @JsonProperty("vip_levels_id")
    private Integer vipLevelsId;

    @Schema(description = "頭像URL", example = "/member/member_images/user-img.svg")
    @JsonProperty("image_url")
    private String imageUrl;

    @Schema(description = "信箱是否已驗證", example = "false")
    @JsonProperty("email_validated")
    private Boolean emailValidated;

    @Schema(description = "創建時間")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "更新時間")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.realname = user.getRealname();
        this.phone = user.getPhone();
        this.gender = user.getGender();
        this.birthDate = user.getBirthDate();
        this.vipLevelsId = user.getVipLevelsId();
        this.imageUrl = user.getImageUrl();
        this.emailValidated = user.getEmailValidated();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}
