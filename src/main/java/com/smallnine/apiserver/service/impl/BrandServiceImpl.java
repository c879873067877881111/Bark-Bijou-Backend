package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.BrandDao;
import com.smallnine.apiserver.dto.BrandRequest;
import com.smallnine.apiserver.dto.BrandResponse;
import com.smallnine.apiserver.entity.Brand;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.utils.SqlSecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BrandServiceImpl {

    private final BrandDao brandDao;

    public Brand findById(Long id) {
        return brandDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.BRAND_NOT_FOUND));
    }

    public List<Brand> findAll(int page, int size) {
        int offset = page * size;
        return brandDao.findAll(offset, size);
    }

    public List<BrandResponse> findAllBrands(int page, int size) {
        List<Brand> brands = findAll(page, size);
        return brands.stream()
                .map(brand -> {
                    BrandResponse response = BrandResponse.fromEntity(brand);
                    response.setProductCount(brandDao.countProductsByBrandId(brand.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<BrandResponse> searchByName(String name, int page, int size) {
        int offset = page * size;
        String safeName = SqlSecurityUtil.escapeLikePattern(name);
        List<Brand> brands = brandDao.searchByName(safeName, offset, size);
        return brands.stream()
                .map(brand -> {
                    BrandResponse response = BrandResponse.fromEntity(brand);
                    response.setProductCount(brandDao.countProductsByBrandId(brand.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Brand createBrand(BrandRequest request) {
        if (brandDao.existsByName(request.getName())) {
            throw new BusinessException(400, "品牌名稱已存在: " + request.getName());
        }

        Brand brand = new Brand();
        brand.setName(request.getName());
        brand.setDescription(request.getDescription());
        brand.setLogoUrl(request.getLogoUrl());
        brand.setCreatedAt(LocalDateTime.now());

        brandDao.insert(brand);
        log.info("action=CREATE_BRAND id={} name={}", brand.getId(), brand.getName());

        return brand;
    }

    @Transactional
    public Brand updateBrand(Long id, BrandRequest request) {
        Brand existingBrand = findById(id);

        if (!request.getName().equals(existingBrand.getName()) &&
                brandDao.existsByName(request.getName())) {
            throw new BusinessException(400, "品牌名稱已存在: " + request.getName());
        }

        existingBrand.setName(request.getName());
        existingBrand.setDescription(request.getDescription());
        existingBrand.setLogoUrl(request.getLogoUrl());

        brandDao.update(existingBrand);
        log.info("action=UPDATE_BRAND id={} name={}", id, existingBrand.getName());

        return existingBrand;
    }

    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = findById(id);

        long productCount = brandDao.countProductsByBrandId(id);
        if (productCount > 0) {
            throw new BusinessException(400, "該品牌下有商品，無法刪除");
        }

        brandDao.deleteById(id);
        log.info("action=DELETE_BRAND id={} name={}", id, brand.getName());
    }

    public long countBrands() {
        return brandDao.count();
    }

    public long countProductsByBrand(Long brandId) {
        return brandDao.countProductsByBrandId(brandId);
    }

    public BrandResponse getBrandResponse(Long id) {
        Brand brand = findById(id);
        BrandResponse response = BrandResponse.fromEntity(brand);
        response.setProductCount(brandDao.countProductsByBrandId(id));
        return response;
    }
}
