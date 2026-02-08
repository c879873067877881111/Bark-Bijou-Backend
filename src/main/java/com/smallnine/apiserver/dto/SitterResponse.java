package com.smallnine.apiserver.dto;

import com.smallnine.apiserver.entity.Sitter;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SitterResponse {

    private Long id;
    private Long memberId;
    private String name;
    private String area;
    private String serviceTime;
    private String experience;
    private String introduction;
    private BigDecimal price;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Double rating;
    private List<String> gallery;
    private List<SitterReviewResponse> reviews;
    private Integer reviewCount;
    private String reviewStatus;

    public static SitterResponse from(Sitter sitter) {
        SitterResponse resp = new SitterResponse();
        resp.setId(sitter.getId());
        resp.setMemberId(sitter.getMemberId());
        resp.setName(sitter.getName());
        resp.setArea(sitter.getArea());
        resp.setServiceTime(sitter.getServiceTime());
        resp.setExperience(sitter.getExperience());
        resp.setIntroduction(sitter.getIntroduction());
        resp.setPrice(sitter.getPrice());
        resp.setAvatarUrl(sitter.getAvatarUrl());
        resp.setCreatedAt(sitter.getCreatedAt());
        resp.setUpdatedAt(sitter.getUpdatedAt());
        return resp;
    }
}
