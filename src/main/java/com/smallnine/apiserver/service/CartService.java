package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dto.CartValidationResult;
import com.smallnine.apiserver.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;

public interface CartService {

    List<CartItem> getCartItems(Long memberId);

    CartItem addToCart(Long memberId, Long productId, Integer quantity);

    CartItem updateCartItemQuantity(Long memberId, Long cartItemId, Integer quantity);

    void removeFromCart(Long memberId, Long cartItemId);

    void clearCart(Long memberId);

    BigDecimal calculateCartTotal(Long memberId);

    long getCartItemCount(Long memberId);

    long countCartItems(Long memberId);

    boolean validateCartStock(Long memberId);

    CartValidationResult validateCart(Long memberId);

    void refreshCartPrices(Long memberId);
}
