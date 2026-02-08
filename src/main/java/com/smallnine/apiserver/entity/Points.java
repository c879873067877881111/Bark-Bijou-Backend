package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Points {
    private Long id;
    private Long memberId;
    private Integer points;
    private String description;
    private Integer referenceId;
    private String referenceType;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
