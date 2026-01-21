package com.smallnine.apiserver.exception;

/**
 * 資源重複異常
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String field, String value) {
        super(resourceName + "已存在: " + field + "=" + value);
    }
}
