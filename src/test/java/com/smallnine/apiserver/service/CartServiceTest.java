package com.smallnine.apiserver.service;

import com.smallnine.apiserver.dao.CartItemDao;
import com.smallnine.apiserver.dao.ProductDao;
import com.smallnine.apiserver.dto.CartValidationResult;
import com.smallnine.apiserver.entity.CartItem;
import com.smallnine.apiserver.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartServiceTest {

    @Autowired private CartService cartService;
    @Autowired private CartItemDao cartItemDao;
    @Autowired private ProductDao productDao;

    private static final Long MEMBER_ID = 1L;
    private static final Long PRODUCT_ID_1 = 1L; // stock=50, price=899, salePrice=799
    private static final Long PRODUCT_ID_2 = 2L; // stock=30, price=299, salePrice=null

    @BeforeEach
    void setUp() {
        cartService.clearCart(MEMBER_ID);
    }

    @Test
    void validateCart_emptyCart() {
        CartValidationResult result = cartService.validateCart(MEMBER_ID);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validateCart_outOfStock() {
        insertCartItem(PRODUCT_ID_1, 9999, new BigDecimal("799.00"));

        CartValidationResult result = cartService.validateCart(MEMBER_ID);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(CartValidationResult.ErrorType.OUT_OF_STOCK, result.getErrors().get(0).getErrorType());
    }

    @Test
    void validateCart_priceChanged() {
        insertCartItem(PRODUCT_ID_1, 1, new BigDecimal("1.00"));

        CartValidationResult result = cartService.validateCart(MEMBER_ID);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(CartValidationResult.ErrorType.PRICE_CHANGED, result.getErrors().get(0).getErrorType());
    }

    @Test
    void validateCart_multipleErrors() {
        insertCartItem(PRODUCT_ID_1, 9999, new BigDecimal("799.00"));
        insertCartItem(PRODUCT_ID_2, 1, new BigDecimal("1.00"));

        CartValidationResult result = cartService.validateCart(MEMBER_ID);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() > 1);
    }

    @Test
    void refreshCartPrices_updatesPrice() {
        insertCartItem(PRODUCT_ID_1, 1, new BigDecimal("1.00"));

        cartService.refreshCartPrices(MEMBER_ID);

        List<CartItem> items = cartService.getCartItems(MEMBER_ID);
        assertEquals(1, items.size());

        Product product = productDao.findById(PRODUCT_ID_1).orElseThrow();
        BigDecimal expectedPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
        assertEquals(0, expectedPrice.compareTo(items.get(0).getUnitPrice()));
    }

    @Test
    void refreshCartPrices_skipsUnchanged() {
        Product product = productDao.findById(PRODUCT_ID_1).orElseThrow();
        BigDecimal correctPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();

        insertCartItem(PRODUCT_ID_1, 1, correctPrice);

        LocalDateTime updatedBefore = cartItemDao.findByMemberId(MEMBER_ID).get(0).getUpdatedAt();

        cartService.refreshCartPrices(MEMBER_ID);

        LocalDateTime updatedAfter = cartItemDao.findByMemberId(MEMBER_ID).get(0).getUpdatedAt();
        assertEquals(updatedBefore, updatedAfter, "價格未變更時不應修改 updatedAt");
    }

    private void insertCartItem(Long productId, int quantity, BigDecimal unitPrice) {
        CartItem item = new CartItem();
        item.setMemberId(MEMBER_ID);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        cartItemDao.insert(item);
    }
}
