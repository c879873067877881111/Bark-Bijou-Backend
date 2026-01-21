package com.smallnine.apiserver.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ArticleRequest {
    
    @NotNull(message = "會員ID不能為空")
    private Long memberId;
    
    @NotBlank(message = "會員用戶名不能為空")
    private String memberUsername;
    
    private String author;
    
    @NotBlank(message = "標題不能為空")
    private String title;
    
    private Long dogsId;
    
    @NotBlank(message = "狗狗品種不能為空")
    private String dogsBreed;
    
    @NotBlank(message = "狗狗圖片不能為空")
    private String dogsImages;
    
    @NotBlank(message = "內容1不能為空")
    private String content1;
    
    @NotBlank(message = "內容2不能為空")
    private String content2;
    
    @NotNull(message = "創建者ID不能為空")
    private Long createdId;
    
    @NotNull(message = "事件ID不能為空")
    private Long eventId;
    
    @NotNull(message = "有效狀態不能為空")
    private Integer valid;
    
    @NotBlank(message = "文章圖片不能為空")
    private String articleImages;
    
    @NotBlank(message = "分類名稱不能為空")
    private String categoryName;
}