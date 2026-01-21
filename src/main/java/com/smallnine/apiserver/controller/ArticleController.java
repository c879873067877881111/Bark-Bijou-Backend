package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.entity.Article;
import com.smallnine.apiserver.service.impl.ArticleServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Tag(name = "文章管理", description = "文章CRUD操作API")
@SecurityRequirement(name = "bearerAuth")
public class ArticleController {

    private final ArticleServiceImpl articleService;

    @GetMapping("/{id}")
    @Operation(summary = "根據ID查詢文章", description = "通過文章ID獲取詳細訊息")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文章不存在")
    })
    public ApiResponse<Article> getArticleById(
            @Parameter(description = "文章ID", required = true)
            @PathVariable Long id) {
        Article article = articleService.findById(id);
        return ApiResponse.success(article);
    }

    @GetMapping
    @Operation(summary = "查詢文章列表", description = "分頁查詢所有文章")
    public ApiResponse<List<Article>> getArticles(
            @Parameter(description = "頁碼", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每頁大小", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        List<Article> articles = articleService.findAll(page, size);
        return ApiResponse.success(articles);
    }

    @GetMapping("/author/{author}")
    @Operation(summary = "根據作者查詢文章", description = "獲取指定作者的所有文章")
    public ApiResponse<List<Article>> getArticlesByAuthor(
            @Parameter(description = "作者名稱", required = true)
            @PathVariable String author) {
        List<Article> articles = articleService.findByAuthor(author);
        return ApiResponse.success(articles);
    }

    @GetMapping("/category/{categoryName}")
    @Operation(summary = "根據分類查詢文章", description = "獲取指定分類的所有文章")
    public ApiResponse<List<Article>> getArticlesByCategory(
            @Parameter(description = "分類名稱", required = true)
            @PathVariable String categoryName) {
        List<Article> articles = articleService.findByCategoryName(categoryName);
        return ApiResponse.success(articles);
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "根據成員ID查詢文章", description = "獲取指定成員的所有文章")
    public ApiResponse<List<Article>> getArticlesByMember(
            @Parameter(description = "成員ID", required = true)
            @PathVariable Long memberId) {
        List<Article> articles = articleService.findByMemberId(memberId);
        return ApiResponse.success(articles);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索文章", description = "根據標題關鍵字搜索文章")
    public ApiResponse<List<Article>> searchArticles(
            @Parameter(description = "搜索關鍵字", required = true)
            @RequestParam String title,
            @Parameter(description = "頁碼", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每頁大小", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        List<Article> articles = articleService.searchByTitle(title, page, size);
        return ApiResponse.success(articles);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @Operation(summary = "創建文章", description = "創建新的文章")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "創建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤")
    })
    public ApiResponse<Article> createArticle(
            @Parameter(description = "文章訊息", required = true)
            @Valid @RequestBody Article article) {
        Article createdArticle = articleService.createArticle(article);
        return ApiResponse.success(createdArticle, ResponseCode.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @articleService.findById(#id).memberId == authentication.principal.id")
    @Operation(summary = "更新文章", description = "更新指定ID的文章")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文章不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限操作")
    })
    public ApiResponse<Article> updateArticle(
            @Parameter(description = "文章ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "更新的文章訊息", required = true)
            @Valid @RequestBody Article article) {
        article.setId(id);
        Article updatedArticle = articleService.updateArticle(article);
        return ApiResponse.success(updatedArticle);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @articleService.findById(#id).memberId == authentication.principal.id")
    @Operation(summary = "刪除文章", description = "刪除指定ID的文章")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "刪除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文章不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限操作")
    })
    public ApiResponse<Void> deleteArticle(
            @Parameter(description = "文章ID", required = true)
            @PathVariable Long id) {
        articleService.deleteArticle(id);
        return ApiResponse.success();
    }

    @GetMapping("/stats/count")
    @Operation(summary = "獲取文章統計", description = "獲取文章總數和有效文章數")
    public ApiResponse<Object> getArticleStats() {
        long total = articleService.count();
        long valid = articleService.countValid();

        return ApiResponse.success(new Object() {
            public final long totalArticles = total;
            public final long validArticles = valid;
        });
    }
}
