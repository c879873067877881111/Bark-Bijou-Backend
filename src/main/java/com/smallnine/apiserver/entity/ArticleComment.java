package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleComment {
    private Long id;
    private Long articleId;
    private Long memberId;
    private String content;
    private LocalDateTime createdAt;
    // joined field
    private String username;
}
