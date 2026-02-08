package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.*;
import com.smallnine.apiserver.entity.Sitter;
import com.smallnine.apiserver.entity.User;
import com.smallnine.apiserver.security.UserPrincipal;
import com.smallnine.apiserver.service.SitterService;
import com.smallnine.apiserver.utils.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sitter")
@RequiredArgsConstructor
public class SitterController {

    private final SitterService sitterService;

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<List<SitterReviewResponse>>> getRecentReviews() {
        List<SitterReviewResponse> reviews = sitterService.getRecentReviews(100);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SitterListResponse>> listSitters(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String area,
            @RequestParam(defaultValue = "rating") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        SitterListResponse result = sitterService.searchSitters(search, area, sort, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/manage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMySitter(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        SitterResponse sitter = sitterService.getMySitter(user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("sitter", sitter != null ? sitter : Map.of())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SitterResponse>> getSitterDetail(@PathVariable Long id) {
        Long memberId = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            memberId = up.getUser().getId();
        }
        SitterResponse sitter = sitterService.getSitterDetail(id, memberId);
        return ResponseEntity.ok(ApiResponse.success(sitter));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Sitter>> createSitter(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SitterRequest request) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Sitter sitter = sitterService.createSitter(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("保母新增成功", sitter));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Sitter>> updateSitter(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SitterRequest request) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Sitter sitter = sitterService.updateSitter(id, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("保母資料已更新", sitter));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSitter(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        sitterService.deleteSitter(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("保母資料已刪除"));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<Void>> addReview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {
        User user = AuthUtils.getAuthenticatedUser(userDetails);
        Integer rating = (Integer) body.get("rating");
        String comment = (String) body.get("comment");
        sitterService.addReview(id, rating, comment, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("評論提交成功"));
    }
}
