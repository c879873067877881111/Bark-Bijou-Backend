package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dto.SitterListResponse;
import com.smallnine.apiserver.dto.SitterRequest;
import com.smallnine.apiserver.dto.SitterResponse;
import com.smallnine.apiserver.dto.SitterReviewResponse;
import com.smallnine.apiserver.entity.Sitter;

import java.util.List;

public interface SitterService {

    List<SitterReviewResponse> getRecentReviews(int limit);

    SitterListResponse searchSitters(String search, String area, String sort, int page, int pageSize);

    SitterResponse getSitterDetail(Long id, Long currentMemberId);

    SitterResponse getMySitter(Long memberId);

    Sitter createSitter(SitterRequest request, Long memberId);

    Sitter updateSitter(Long id, SitterRequest request, Long memberId);

    void deleteSitter(Long id, Long memberId);

    void addReview(Long sitterId, Integer rating, String comment, Long memberId);
}
