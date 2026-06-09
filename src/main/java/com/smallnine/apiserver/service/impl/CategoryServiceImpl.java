package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.CategoryDao;
import com.smallnine.apiserver.dao.ProductDao;
import com.smallnine.apiserver.dto.CategoryRequest;
import com.smallnine.apiserver.dto.CategoryResponse;
import com.smallnine.apiserver.entity.Category;
import com.smallnine.apiserver.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.smallnine.apiserver.service.CategoryService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryDao categoryDao;
    private final ProductDao productDao;

    @Cacheable(value = "categories", key = "'id:' + #id")
    public Category findById(Long id) {
        return categoryDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.CATEGORY_NOT_FOUND));
    }

    public List<Category> findAll(int page, int size) {
        int offset = page * size;
        return categoryDao.findAll(offset, size);
    }

    public List<Category> findActiveCategories(int page, int size) {
        int offset = page * size;
        return categoryDao.findActiveCategories(offset, size);
    }

    // 列表查詢不快取：任一筆分類異動都會讓 top/tree/parent 列表 stale，難精準失效，故不快取
    public List<CategoryResponse> findTopCategories() {
        List<Category> topCategories = categoryDao.findTopCategories();
        List<CategoryResponse> responses = new ArrayList<>();
        for (Category category : topCategories) {
            CategoryResponse response = CategoryResponse.fromEntity(category);
            response.setProductCount(productDao.countByCategoryId(category.getId()));
            responses.add(response);
        }
        return responses;
    }

    // 列表查詢不快取：樹狀結果橫跨多筆分類，任一筆異動即整棵 stale，難精準失效
    public List<CategoryResponse> getCategoryTree() {
        List<Category> topCategories = categoryDao.findTopCategories();
        List<CategoryResponse> responses = new ArrayList<>();
        for (Category category : topCategories) {
            responses.add(buildCategoryTree(category));
        }
        return responses;
    }

    // 列表查詢不快取：同一 parentId 下子分類列表會隨子分類增減/異動 stale，難精準失效
    public List<CategoryResponse> findByParentId(Long parentId) {
        List<Category> categories = categoryDao.findByParentId(parentId);
        List<CategoryResponse> responses = new ArrayList<>();
        for (Category category : categories) {
            CategoryResponse response = CategoryResponse.fromEntity(category);
            response.setProductCount(productDao.countByCategoryId(category.getId()));
            responses.add(response);
        }
        return responses;
    }



    // 新增不需 evict：id 為新生成，不可能有對應的舊 by-id cache；列表已不快取，故無 allEntries 需求
    @Transactional
    public Category createCategory(CategoryRequest request) {
        if (categoryDao.existsByName(request.getName())) {
            throw new BusinessException(ResponseCode.CONFLICT, "分類名稱已存在: " + request.getName());
        }

        if (request.getParentId() != null) {
            Category parentCategory = findById(request.getParentId());
            if (!parentCategory.getIsActive()) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "父分類未啟用，無法添加子分類");
            }
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setParentId(request.getParentId());
        category.setImageUrl(request.getImageUrl());
        category.setIsActive(request.getIsActive());
        category.setCreatedAt(LocalDateTime.now());

        categoryDao.insert(category);
        log.info("action=CREATE_CATEGORY id={} name={} parentId={}",
                category.getId(), category.getName(), category.getParentId());

        return category;
    }

    // 精準 evict：只清掉該筆 id 的 by-id cache（findById 的 'id:'），不再 allEntries；列表已不快取無 stale 之虞
    @CacheEvict(value = "categories", key = "'id:' + #id")
    @Transactional
    public Category updateCategory(Long id, CategoryRequest request) {
        Category existingCategory = findById(id);

        if (!request.getName().equals(existingCategory.getName()) &&
                categoryDao.existsByName(request.getName())) {
            throw new BusinessException(ResponseCode.CONFLICT, "分類名稱已存在: " + request.getName());
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "分類不能設置自己為父分類");
            }
            Category parentCategory = findById(request.getParentId());
            if (!parentCategory.getIsActive()) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "父分類未啟用，無法設置為父分類");
            }
        }

        existingCategory.setName(request.getName());
        existingCategory.setDescription(request.getDescription());
        existingCategory.setParentId(request.getParentId());
        existingCategory.setImageUrl(request.getImageUrl());
        existingCategory.setIsActive(request.getIsActive());

        categoryDao.update(existingCategory);
        log.info("action=UPDATE_CATEGORY id={} name={}", id, existingCategory.getName());

        return existingCategory;
    }

    // 精準 evict：刪除時清掉該筆 id 的 by-id cache，不再 allEntries
    @CacheEvict(value = "categories", key = "'id:' + #id")
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findById(id);

        if (categoryDao.hasChildren(id)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "該分類下有子分類，無法刪除");
        }

        long productCount = productDao.countByCategoryId(id);
        if (productCount > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "該分類下有商品，無法刪除");
        }

        categoryDao.deleteById(id);
        log.info("action=DELETE_CATEGORY id={} name={}", id, category.getName());
    }

    // 精準 evict：狀態更新只影響該筆，清掉其 by-id cache 即可，不再 allEntries
    @CacheEvict(value = "categories", key = "'id:' + #id")
    @Transactional
    public void updateCategoryStatus(Long id, Boolean isActive) {
        Category category = findById(id);

        if (!isActive && categoryDao.hasChildren(id)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "該分類下有子分類，無法停用");
        }

        int updatedRows = categoryDao.updateStatus(id, isActive);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, "分類狀態更新失敗");
        }

        log.info("action=UPDATE_CATEGORY_STATUS id={} name={} isActive={}", id, category.getName(), isActive);
    }

    public long countCategories() {
        return categoryDao.count();
    }

    public long countActiveCategories() {
        return categoryDao.countActive();
    }

    private CategoryResponse buildCategoryTree(Category category) {
        CategoryResponse response = CategoryResponse.fromEntity(category);
        response.setProductCount(productDao.countByCategoryId(category.getId()));

        List<Category> children = categoryDao.findByParentId(category.getId());
        if (!children.isEmpty()) {
            List<CategoryResponse> childResponses = new ArrayList<>();
            for (Category child : children) {
                childResponses.add(buildCategoryTree(child));
            }
            response.setChildren(childResponses);
        }

        return response;
    }
}
