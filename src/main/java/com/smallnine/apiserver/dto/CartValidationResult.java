package com.smallnine.apiserver.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CartValidationResult {

    private boolean valid = true;
    private List<CartError> errors = new ArrayList<>();

    public void addError(ErrorType errorType, Long productId, String message) {
        errors.add(new CartError(errorType, productId, message));
        valid = false;
    }

    public enum ErrorType {
        OUT_OF_STOCK,
        PRICE_CHANGED,
        PRODUCT_NOT_FOUND,
        PRODUCT_INACTIVE
    }

    @Data
    public static class CartError {
        private final ErrorType errorType;
        private final Long productId;
        private final String message;
    }
}
