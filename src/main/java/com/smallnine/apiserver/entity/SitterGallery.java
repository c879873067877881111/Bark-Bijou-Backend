package com.smallnine.apiserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SitterGallery {

    private Long id;
    private Long sitterId;
    private String imageUrl;
    private LocalDateTime createdAt;
}
