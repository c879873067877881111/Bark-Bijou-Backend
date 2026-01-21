package com.smallnine.apiserver.exception;

/**
 * 資源不存在異常
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + "不存在: " + id);
    }

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(resourceName + "不存在: " + identifier);
    }
}
