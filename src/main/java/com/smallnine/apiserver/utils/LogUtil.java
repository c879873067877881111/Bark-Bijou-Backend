package com.smallnine.apiserver.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 日誌工具類
 * 日誌記錄方法和MDC管理
 */
public class LogUtil {
    
    private static final String TRACE_ID = "traceId";
    private static final String USER_ID = "userId";
    private static final String REQUEST_URI = "requestUri";
    private static final String REQUEST_METHOD = "requestMethod";
    private static final String CLIENT_IP = "clientIp";
    
    /**
     * 設置追蹤ID
     */
    public static void setTraceId() {
        setTraceId(UUID.randomUUID().toString().replace("-", ""));
    }
    
    /**
     * 設置指定的追蹤ID
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }
    
    /**
     * 獲取追蹤ID
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }
    
    /**
     * 設置用戶ID
     */
    public static void setUserId(String userId) {
        MDC.put(USER_ID, userId);
    }
    
    /**
     * 獲取用戶ID
     */
    public static String getUserId() {
        return MDC.get(USER_ID);
    }
    
    /**
     * 設置請求信息
     */
    public static void setRequestInfo(String method, String uri, String clientIp) {
        MDC.put(REQUEST_METHOD, method);
        MDC.put(REQUEST_URI, uri);
        MDC.put(CLIENT_IP, clientIp);
    }
    
    /**
     * 清除所有MDC信息
     */
    public static void clear() {
        MDC.clear();
    }
    
    /**
     * 記錄業務操作日誌
     */
    public static void logBusiness(Logger logger, String operation, String details) {
        logger.info("業務操作: {} - {}", operation, details);
    }
    
    /**
     * 記錄API調用日誌
     */
    public static void logApiCall(Logger logger, String method, String uri, long duration) {
        logger.info("API調用: {} {} - 耗時: {}ms", method, uri, duration);
    }
    
    /**
     * 記錄數據庫操作日誌
     */
    public static void logDatabase(Logger logger, String operation, String table, Object id) {
        logger.debug("數據庫操作: {} - 表: {} - ID: {}", operation, table, id);
    }
    
    /**
     * 記錄異常日誌
     */
    public static void logError(Logger logger, String operation, Exception e) {
        logger.error("操作失敗: {} - 錯誤: {}", operation, e.getMessage(), e);
    }
    
    /**
     * 記錄性能日誌
     */
    public static void logPerformance(Logger logger, String operation, long duration, Object... params) {
        if (duration > 1000) {
            logger.warn("性能警告: {} - 耗時: {}ms - 參數: {}", operation, duration, params);
        } else {
            logger.debug("性能記錄: {} - 耗時: {}ms - 參數: {}", operation, duration, params);
        }
    }
    
    /**
     * 記錄安全相關日誌
     */
    public static void logSecurity(Logger logger, String event, String userId, String clientIp) {
        logger.info("安全事件: {} - 用戶: {} - IP: {}", event, userId, clientIp);
    }
}