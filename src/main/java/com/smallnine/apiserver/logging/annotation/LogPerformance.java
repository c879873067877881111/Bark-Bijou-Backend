package com.smallnine.apiserver.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 性能日誌註解
 * 自動記錄方法執行時間
 *
 * 使用示例：
 * <pre>
 * {@code @LogPerformance(operation = "批量導入商品", threshold = 5000)}
 * public void batchImport(List<Product> products) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogPerformance {

    /**
     * 操作名稱
     */
    String operation() default "";

    /**
     * 慢操作閾值（毫秒），超過此值會記錄 WARN 日誌
     * 默認 1000ms
     */
    long threshold() default 1000L;

    /**
     * 是否總是記錄（即使沒有超過閾值）
     */
    boolean alwaysLog() default false;
}
