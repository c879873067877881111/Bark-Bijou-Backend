package com.smallnine.apiserver.service.impl;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dao.CartItemDao;
import com.smallnine.apiserver.dao.ProductDao;
import com.smallnine.apiserver.dto.CartValidationResult;
import com.smallnine.apiserver.entity.CartItem;
import com.smallnine.apiserver.entity.Product;
import com.smallnine.apiserver.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.smallnine.apiserver.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {
    
    private final CartItemDao cartItemDao;
    private final ProductDao productDao;
    
    /**
     * 查詢用戶購物車
     */
    public List<CartItem> getCartItems(Long memberId) {
        return cartItemDao.findByMemberId(memberId);
    }
    
    /**
     * 添加商品到購物車
     */
    @Transactional
    public CartItem addToCart(Long memberId, Long productId, Integer quantity) {
        log.info("添加商品到購物車: memberId={}, productId={}, quantity={}", 
                 memberId, productId, quantity);
        
        if (quantity <= 0) {
            throw new BusinessException(ResponseCode.INVALID_QUANTITY, "商品數量必須大於0");
        }
        
        // 驗證商品是否存在且可用
        Product product = productDao.findById(productId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PRODUCT_NOT_FOUND));
        
        if (!product.getIsActive()) {
            throw new BusinessException(ResponseCode.PRODUCT_INACTIVE);
        }
        
        // 檢查庫存
        if (product.getStockQuantity() < quantity) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
        
        // 檢查是否已存在相同商品
        Optional<CartItem> existingItem = cartItemDao.findByMemberIdAndProductId(memberId, productId);
        
        if (existingItem.isPresent()) {
            // 更新數量
            CartItem cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + quantity;
            
            // 再次檢查庫存
            if (product.getStockQuantity() < newQuantity) {
                throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
            }
            
            cartItem.setQuantity(newQuantity);
            cartItem.setUpdatedAt(LocalDateTime.now());
            cartItemDao.update(cartItem);
            
            log.info("購物車商品數量更新: cartItemId={}, newQuantity={}", 
                     cartItem.getId(), newQuantity);
            return cartItem;
        } else {
            // 創建新的購物車項目
            CartItem cartItem = new CartItem();
            cartItem.setMemberId(memberId);
            cartItem.setProductId(productId);
            cartItem.setQuantity(quantity);
            cartItem.setUnitPrice(product.getSalePrice() != null ? product.getSalePrice() : product.getPrice());
            cartItem.setCreatedAt(LocalDateTime.now());
            cartItem.setUpdatedAt(LocalDateTime.now());
            
            cartItemDao.insert(cartItem);
            
            log.info("新商品添加到購物車: cartItemId={}", cartItem.getId());
            return cartItem;
        }
    }
    
    /**
     * 更新購物車商品數量
     */
    @Transactional
    public CartItem updateCartItemQuantity(Long memberId, Long cartItemId, Integer quantity) {
        log.info("更新購物車商品數量: memberId={}, cartItemId={}, quantity={}", 
                 memberId, cartItemId, quantity);
        
        if (quantity <= 0) {
            throw new BusinessException(ResponseCode.INVALID_QUANTITY, "商品數量必須大於0");
        }
        
        CartItem cartItem = cartItemDao.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ResponseCode.NOT_FOUND, "購物車項目不存在"));
        
        // 驗證購物車項目屬於當前用戶
        if (!cartItem.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "無權限操作此購物車項目");
        }
        
        // 驗證商品庫存
        Product product = productDao.findById(cartItem.getProductId())
                .orElseThrow(() -> new BusinessException(ResponseCode.PRODUCT_NOT_FOUND));
        
        if (product.getStockQuantity() < quantity) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
        
        cartItem.setQuantity(quantity);
        cartItem.setUpdatedAt(LocalDateTime.now());
        cartItemDao.update(cartItem);
        
        log.info("購物車商品數量更新成功: cartItemId={}, newQuantity={}", 
                 cartItemId, quantity);
        return cartItem;
    }
    
    /**
     * 從購物車移除商品
     */
    @Transactional
    public void removeFromCart(Long memberId, Long cartItemId) {
        log.info("從購物車移除商品: memberId={}, cartItemId={}", memberId, cartItemId);
        
        CartItem cartItem = cartItemDao.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ResponseCode.NOT_FOUND, "購物車項目不存在"));
        
        // 驗證購物車項目屬於當前用戶
        if (!cartItem.getMemberId().equals(memberId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "無權限操作此購物車項目");
        }
        
        cartItemDao.deleteById(cartItemId);
        log.info("商品已從購物車移除: cartItemId={}", cartItemId);
    }
    
    /**
     * 清空購物車
     */
    @Transactional
    public void clearCart(Long memberId) {
        log.info("清空購物車: memberId={}", memberId);
        
        int deletedCount = cartItemDao.deleteByMemberId(memberId);
        log.info("購物車已清空: memberId={}, 移除商品數量={}", memberId, deletedCount);
    }
    
    /**
     * 計算購物車總金額
     */
    public BigDecimal calculateCartTotal(Long memberId) {
        List<CartItem> cartItems = cartItemDao.findByMemberId(memberId);
        
        return cartItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 統計購物車商品數量
     */
    public long getCartItemCount(Long memberId) {
        return cartItemDao.countByMemberId(memberId);
    }
    
    /**
     * 統計購物車商品數量（別名方法）
     */
    public long countCartItems(Long memberId) {
        return getCartItemCount(memberId);
    }
    
    /**
     * 驗證購物車商品庫存
     */
    public boolean validateCartStock(Long memberId) {
        List<CartItem> cartItems = cartItemDao.findByMemberId(memberId);
        
        for (CartItem item : cartItems) {
            Optional<Product> productOpt = productDao.findById(item.getProductId());
            if (productOpt.isEmpty()) {
                log.warn("購物車中商品不存在: productId={}", item.getProductId());
                return false;
            }
            
            Product product = productOpt.get();
            if (!product.getIsActive()) {
                log.warn("購物車中商品已下架: productId={}", item.getProductId());
                return false;
            }
            
            if (product.getStockQuantity() < item.getQuantity()) {
                log.warn("購物車中商品庫存不足: productId={}, required={}, available={}", 
                         item.getProductId(), item.getQuantity(), product.getStockQuantity());
                return false;
            }
        }
        
        return true;
    }

    @Override
    public CartValidationResult validateCart(Long memberId) {
        CartValidationResult result = new CartValidationResult();
        List<CartItem> cartItems = cartItemDao.findByMemberId(memberId);

        for (CartItem item : cartItems) {
            Optional<Product> productOpt = productDao.findById(item.getProductId());
            if (productOpt.isEmpty()) {
                result.addError(CartValidationResult.ErrorType.PRODUCT_NOT_FOUND,
                        item.getProductId(), "商品不存在");
                continue;
            }

            Product product = productOpt.get();
            if (!product.getIsActive()) {
                result.addError(CartValidationResult.ErrorType.PRODUCT_INACTIVE,
                        item.getProductId(), "商品已下架");
                continue;
            }

            if (product.getStockQuantity() < item.getQuantity()) {
                result.addError(CartValidationResult.ErrorType.OUT_OF_STOCK,
                        item.getProductId(), "庫存不足");
            }

            BigDecimal currentPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
            if (currentPrice.compareTo(item.getUnitPrice()) != 0) {
                result.addError(CartValidationResult.ErrorType.PRICE_CHANGED,
                        item.getProductId(), "價格已變更");
            }
        }

        return result;
    }

    @Override
    @Transactional
    public void refreshCartPrices(Long memberId) {
        List<CartItem> cartItems = cartItemDao.findByMemberId(memberId);

        for (CartItem item : cartItems) {
            productDao.findById(item.getProductId()).ifPresent(product -> {
                BigDecimal currentPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
                if (currentPrice.compareTo(item.getUnitPrice()) != 0) {
                    item.setUnitPrice(currentPrice);
                    item.setUpdatedAt(LocalDateTime.now());
                    cartItemDao.update(item);
                }
            });
        }
    }
}