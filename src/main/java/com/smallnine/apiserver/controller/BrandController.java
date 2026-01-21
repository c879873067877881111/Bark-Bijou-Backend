package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.dto.BrandRequest;
import com.smallnine.apiserver.dto.BrandResponse;
import com.smallnine.apiserver.entity.Brand;
import com.smallnine.apiserver.service.impl.BrandServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
@Tag(name = "品牌管理", description = "品牌相關 API - 查詢、創建、更新、刪除品牌")
public class BrandController {

    private final BrandServiceImpl brandService;

    @Operation(summary = "獲取所有品牌", description = "分頁獲取品牌列表")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<BrandResponse> brands = brandService.findAllBrands(page, size);
        return ResponseEntity.ok(ApiResponse.success(brands));
    }

    @Operation(summary = "根據ID獲取品牌", description = "根據品牌ID獲取單個品牌詳情")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功獲取品牌"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "品牌不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable Long id) {
        BrandResponse brand = brandService.getBrandResponse(id);
        return ResponseEntity.ok(ApiResponse.success(brand));
    }

    @Operation(summary = "搜索品牌", description = "根據品牌名稱搜索品牌")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<BrandResponse>>> searchBrands(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<BrandResponse> brands = brandService.searchByName(name, page, size);
        return ResponseEntity.ok(ApiResponse.success(brands));
    }

    @Operation(summary = "創建品牌", description = "創建新品牌")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "品牌創建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數無效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<Brand>> createBrand(@Valid @RequestBody BrandRequest request) {
        Brand brand = brandService.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("品牌創建成功", brand));
    }

    @Operation(summary = "更新品牌", description = "根據ID更新品牌訊息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "品牌更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數無效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "品牌不存在")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Brand>> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandRequest request) {
        Brand brand = brandService.updateBrand(id, request);
        return ResponseEntity.ok(ApiResponse.success("品牌更新成功", brand));
    }

    @Operation(summary = "刪除品牌", description = "根據ID刪除品牌")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "品牌刪除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授權"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "品牌不存在")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success("品牌刪除成功"));
    }

    @Operation(summary = "統計品牌數量", description = "獲取品牌總數")
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getBrandCount() {
        long count = brandService.countBrands();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @Operation(summary = "統計品牌商品數量", description = "獲取指定品牌下的商品數量")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "統計成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "品牌不存在")
    })
    @GetMapping("/{id}/products/count")
    public ResponseEntity<ApiResponse<Long>> getBrandProductCount(@PathVariable Long id) {
        brandService.findById(id);
        long count = brandService.countProductsByBrand(id);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
