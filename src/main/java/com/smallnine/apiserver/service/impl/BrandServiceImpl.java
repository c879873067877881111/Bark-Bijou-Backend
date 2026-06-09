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
import com.smallnine.apiserver.service.BrandService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BrandServiceImpl implements BrandService {

    private final BrandDao brandDao;

    @Cacheable(value = "brands", key = "'id:' + #id")
    public Brand findById(Long id) {
        return brandDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.BRAND_NOT_FOUND));
    }

    // 列表查詢不快取：分頁列表 key 會隨 page/size 組合爆量，且新增/刪除任一筆都會讓所有分頁 stale，難以精準失效
    public List<Brand> findAll(int page, int size) {
        int offset = page * size;
        return brandDao.findAll(offset, size);
    }

    // 列表查詢不快取：同上，避免列表 cache 爆量與寫入後讀到舊資料
    public List<BrandResponse> findAllBrands(int page, int size) {
        List<Brand> brands = findAll(page, size);
        List<BrandResponse> responses = new ArrayList<>();
        for (Brand brand : brands) {
            BrandResponse response = BrandResponse.fromEntity(brand);
            response.setProductCount(brandDao.countProductsByBrandId(brand.getId()));
            responses.add(response);
        }
        return responses;
    }

    public List<BrandResponse> searchByName(String name, int page, int size) {
        int offset = page * size;
        String safeName = SqlSecurityUtil.escapeLikePattern(name);
        List<Brand> brands = brandDao.searchByName(safeName, offset, size);
        List<BrandResponse> responses = new ArrayList<>();
        for (Brand brand : brands) {
            BrandResponse response = BrandResponse.fromEntity(brand);
            response.setProductCount(brandDao.countProductsByBrandId(brand.getId()));
            responses.add(response);
        }
        return responses;
    }

    // 新增不需 evict：id 為新生成，不可能有對應的舊 by-id cache；列表已不快取，故無 allEntries 需求
    @Transactional
    public Brand createBrand(BrandRequest request) {
        if (brandDao.existsByName(request.getName())) {
            throw new BusinessException(ResponseCode.CONFLICT, "品牌名稱已存在: " + request.getName());
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

    // 精準 evict：只清掉該筆 id 的兩個 by-id cache（findById 的 'id:' 與 getBrandResponse 的 'response:'），不再 allEntries
    @Caching(evict = {
            @CacheEvict(value = "brands", key = "'id:' + #id"),
            @CacheEvict(value = "brands", key = "'response:' + #id")
    })
    @Transactional
    public Brand updateBrand(Long id, BrandRequest request) {
        Brand existingBrand = findById(id);

        if (!request.getName().equals(existingBrand.getName()) &&
                brandDao.existsByName(request.getName())) {
            throw new BusinessException(ResponseCode.CONFLICT, "品牌名稱已存在: " + request.getName());
        }

        existingBrand.setName(request.getName());
        existingBrand.setDescription(request.getDescription());
        existingBrand.setLogoUrl(request.getLogoUrl());

        brandDao.update(existingBrand);
        log.info("action=UPDATE_BRAND id={} name={}", id, existingBrand.getName());

        return existingBrand;
    }

    // 精準 evict：刪除時清掉該筆 id 的兩個 by-id cache，不再 allEntries
    @Caching(evict = {
            @CacheEvict(value = "brands", key = "'id:' + #id"),
            @CacheEvict(value = "brands", key = "'response:' + #id")
    })
    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = findById(id);

        long productCount = brandDao.countProductsByBrandId(id);
        if (productCount > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "該品牌下有商品，無法刪除");
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

    @Cacheable(value = "brands", key = "'response:' + #id")
    public BrandResponse getBrandResponse(Long id) {
        Brand brand = findById(id);
        BrandResponse response = BrandResponse.fromEntity(brand);
        response.setProductCount(brandDao.countProductsByBrandId(id));
        return response;
    }
}
