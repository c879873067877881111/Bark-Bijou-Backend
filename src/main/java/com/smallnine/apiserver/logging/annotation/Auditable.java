package com.smallnine.apiserver.logging.annotation;

import com.smallnine.apiserver.logging.constants.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 審計日誌註解
 * 標記需要記錄審計日誌的方法
 *
 * 使用示例：
 * <pre>
 * {@code @Auditable(action = AuditAction.CREATE, resource = "Product")}
 * public Product createProduct(ProductRequest request) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * 審計操作類型
     */
    AuditAction action();

    /**
     * 資源類型（如 "Product", "Order", "User"）
     */
    String resource();

    /**
     * SpEL 表達式，用於從方法參數或返回值中提取資源ID
     * 例如: "#result.id" 或 "#id" 或 "#request.productId"
     */
    String resourceId() default "";

    /**
     * 操作描述（可選）
     */
    String description() default "";

    /**
     * 是否記錄方法參數
     */
    boolean logParams() default false;

    /**
     * 是否記錄返回值
     */
    boolean logResult() default false;
}
