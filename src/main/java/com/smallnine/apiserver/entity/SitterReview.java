package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SitterReview {

    private Long id;
    private Long memberId;
    private Long sitterId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
