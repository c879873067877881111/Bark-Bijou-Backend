package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.ProductDao;
import com.smallnine.apiserver.dto.ProductDTO;
import com.smallnine.apiserver.dto.ProductRequest;
import com.smallnine.apiserver.entity.Product;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.logging.annotation.Auditable;
import com.smallnine.apiserver.logging.constants.AuditAction;
import com.smallnine.apiserver.utils.SqlSecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl {

    private final ProductDao productDao;

    public Product findById(Long id) {
        return productDao.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.PRODUCT_NOT_FOUND));
    }

    public List<Product> findActiveProducts(int page, int size) {
        int offset = page * size;
        return productDao.findActiveProducts(offset, size);
    }

    public List<Product> findAll(int page, int size) {
        int offset = page * size;
        return productDao.findAll(offset, size);
    }

    public List<Product> findByCategoryId(Long categoryId) {
        return productDao.findByCategoryId(categoryId);
    }

    public List<Product> findByCategory(Long categoryId, int page, int size) {
        int offset = page * size;
        return productDao.findByCategoryIdWithPaging(categoryId, offset, size);
    }

    public List<Product> findByBrandId(Long brandId) {
        return productDao.findByBrandId(brandId);
    }

    public List<Product> searchByName(String name, int page, int size) {
        int offset = page * size;
        String safeName = SqlSecurityUtil.escapeLikePattern(name);
        return productDao.searchByName(safeName, offset, size);
    }

    public List<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice,
                                          int page, int size) {
        int offset = page * size;
        return productDao.findByPriceRange(minPrice, maxPrice, offset, size);
    }

    public List<Product> searchProducts(String keyword, BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        int offset = page * size;
        String safeKeyword = SqlSecurityUtil.escapeLikePattern(keyword);
        return productDao.searchProducts(safeKeyword, minPrice, maxPrice, offset, size);
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        ProductDTO productDTO = convertRequestToDTO(request);
        return createProduct(productDTO);
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, resource = "Product", resourceId = "#result.id")
    public Product createProduct(ProductDTO productDTO) {
        if (productDTO.getSku() != null && productDao.existsBySku(productDTO.getSku())) {
            throw new BusinessException(400, "SKU已存在: " + productDTO.getSku());
        }

        Product product = convertToEntity(productDTO);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        productDao.insert(product);
        // 審計日誌由 @Auditable AOP 自動記錄
        log.debug("action=CREATE_PRODUCT id={} name={} sku={}",
                product.getId(), product.getName(), product.getSku());

        return product;
    }

    @Transactional
    public Product updateProduct(Long id, ProductRequest request) {
        ProductDTO productDTO = convertRequestToDTO(request);
        return updateProduct(id, productDTO);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, resource = "Product", resourceId = "#id")
    public Product updateProduct(Long id, ProductDTO productDTO) {
        Product existingProduct = findById(id);

        if (productDTO.getSku() != null && !productDTO.getSku().equals(existingProduct.getSku())) {
            if (productDao.existsBySku(productDTO.getSku())) {
                throw new BusinessException(400, "SKU已存在: " + productDTO.getSku());
            }
        }

        updateEntityFromDTO(existingProduct, productDTO);
        existingProduct.setUpdatedAt(LocalDateTime.now());

        productDao.update(existingProduct);
        log.debug("action=UPDATE_PRODUCT id={} name={}", id, existingProduct.getName());

        return existingProduct;
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, resource = "Product", resourceId = "#id")
    public void deleteProduct(Long id) {
        Product product = findById(id);
        productDao.deleteById(id);
        log.debug("action=DELETE_PRODUCT id={} name={}", id, product.getName());
    }

    @Transactional
    public boolean updateStock(Long id, Integer stockQuantity) {
        try {
            updateStockInternal(id, stockQuantity);
            return true;
        } catch (Exception e) {
            log.error("action=UPDATE_STOCK_FAILED id={} stock={} error={}", id, stockQuantity, e.getMessage());
            return false;
        }
    }

    @Transactional
    public void updateStockInternal(Long id, Integer stockQuantity) {
        if (stockQuantity < 0) {
            throw new BusinessException(400, "庫存數量不能為負");
        }

        Product product = findById(id);
        Integer oldStock = product.getStockQuantity();

        int updatedRows = productDao.updateStock(id, stockQuantity);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, "庫存更新失敗");
        }

        log.info("action=UPDATE_STOCK id={} name={} oldStock={} newStock={}",
                id, product.getName(), oldStock, stockQuantity);
    }

    public boolean checkStock(Long id, int quantity) {
        Product product = findById(id);
        return product.getStockQuantity() >= quantity;
    }

    @Transactional
    public boolean decreaseStock(Long id, Integer quantity) {
        if (quantity <= 0) {
            throw new BusinessException(400, "減少數量必須大於0");
        }

        int updatedRows = productDao.decreaseStock(id, quantity);
        if (updatedRows == 0) {
            log.warn("action=DECREASE_STOCK_FAILED id={} quantity={} reason=insufficient_stock", id, quantity);
            return false;
        }

        log.info("action=DECREASE_STOCK id={} quantity={}", id, quantity);
        return true;
    }

    public long countProducts() {
        return productDao.count();
    }

    public long countByCategoryId(Long categoryId) {
        return productDao.countByCategoryId(categoryId);
    }

    private Product convertToEntity(ProductDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setSalePrice(dto.getSalePrice());
        product.setSku(dto.getSku());
        product.setStockQuantity(dto.getStockQuantity());
        product.setBrandId(dto.getBrandId());
        product.setCategoryId(dto.getCategoryId());
        product.setIsActive(dto.getIsActive());
        product.setWeight(dto.getWeight());
        product.setDimensions(dto.getDimensions());
        return product;
    }

    private void updateEntityFromDTO(Product product, ProductDTO dto) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setSalePrice(dto.getSalePrice());
        product.setSku(dto.getSku());
        product.setStockQuantity(dto.getStockQuantity());
        product.setBrandId(dto.getBrandId());
        product.setCategoryId(dto.getCategoryId());
        product.setIsActive(dto.getIsActive());
        product.setWeight(dto.getWeight());
        product.setDimensions(dto.getDimensions());
    }

    private ProductDTO convertRequestToDTO(ProductRequest request) {
        ProductDTO dto = new ProductDTO();
        dto.setName(request.getName());
        dto.setDescription(request.getDescription());
        dto.setPrice(request.getPrice());
        dto.setSalePrice(request.getSalePrice());
        dto.setSku(request.getSku());
        dto.setStockQuantity(request.getStockQuantity());
        dto.setBrandId(request.getBrandId());
        dto.setCategoryId(request.getCategoryId());
        dto.setIsActive(request.getIsActive());
        dto.setWeight(request.getWeight());
        dto.setDimensions(request.getDimensions());
        return dto;
    }
}
