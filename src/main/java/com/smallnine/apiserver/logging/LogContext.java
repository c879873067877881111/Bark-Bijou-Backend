package com.smallnine.apiserver.logging;

import com.smallnine.apiserver.logging.constants.LogConstants;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 日誌上下文管理
 * 統一管理 MDC，確保每條日誌都能追蹤
 */
public final class LogContext {

    private LogContext() {
    }

    /**
     * 初始化請求上下文（通常在 Interceptor 中調用）
     */
    public static String initRequest(String method, String uri, String clientIp) {
        String traceId = generateTraceId();
        MDC.put(LogConstants.TRACE_ID, traceId);
        MDC.put(LogConstants.SPAN_ID, generateSpanId());
        MDC.put(LogConstants.REQUEST_METHOD, method);
        MDC.put(LogConstants.REQUEST_URI, uri);
        MDC.put(LogConstants.CLIENT_IP, clientIp);
        return traceId;
    }

    /**
     * 設置用戶信息（通常在認證後調用）
     */
    public static void setUser(String userId, String username) {
        if (userId != null) MDC.put(LogConstants.USER_ID, userId);
        if (username != null) MDC.put(LogConstants.USERNAME, username);
    }

    /**
     * 設置用戶ID
     */
    public static void setUserId(Long userId) {
        if (userId != null) {
            MDC.put(LogConstants.USER_ID, userId.toString());
        }
    }

    /**
     * 使用外部 traceId（例如從 header 傳入）
     */
    public static void setTraceId(String traceId) {
        MDC.put(LogConstants.TRACE_ID, traceId);
    }

    /**
     * 獲取當前 traceId
     */
    public static String getTraceId() {
        return MDC.get(LogConstants.TRACE_ID);
    }

    /**
     * 獲取當前用戶ID
     */
    public static String getUserId() {
        return MDC.get(LogConstants.USER_ID);
    }

    /**
     * 獲取當前用戶名
     */
    public static String getUsername() {
        return MDC.get(LogConstants.USERNAME);
    }

    /**
     * 獲取客戶端IP
     */
    public static String getClientIp() {
        return MDC.get(LogConstants.CLIENT_IP);
    }

    /**
     * 清除所有上下文（請求結束時調用）
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * 生成 TraceId (32位)
     */
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成 SpanId (16位)
     */
    private static String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
