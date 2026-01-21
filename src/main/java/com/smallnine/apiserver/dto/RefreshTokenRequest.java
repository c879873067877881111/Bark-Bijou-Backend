package com.smallnine.apiserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新令牌請求")
public class RefreshTokenRequest {
    
    @NotBlank(message = "更新令牌不能為空")
    @Schema(description = "更新令牌", example = "abc123-def456-ghi789...", required = true)
    private String refreshToken;
}