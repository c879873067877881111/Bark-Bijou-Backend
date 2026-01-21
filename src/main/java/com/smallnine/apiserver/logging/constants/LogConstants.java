package com.smallnine.apiserver.logging.constants;

/**
 * 日誌系統常量定義
 * 企業級日誌規範
 */
public final class LogConstants {

    private LogConstants() {
    }

    // ===== MDC Keys =====
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
    public static final String CLIENT_IP = "clientIp";
    public static final String REQUEST_URI = "requestUri";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String SESSION_ID = "sessionId";

    // ===== Logger Names =====
    public static final String AUDIT_LOGGER = "AUDIT";
    public static final String SECURITY_LOGGER = "SECURITY";
    public static final String PERFORMANCE_LOGGER = "PERFORMANCE";
    public static final String BUSINESS_LOGGER = "BUSINESS";

    // ===== 性能閾值 (毫秒) =====
    public static final long SLOW_QUERY_THRESHOLD = 1000L;
    public static final long SLOW_API_THRESHOLD = 2000L;
    public static final long VERY_SLOW_THRESHOLD = 5000L;
}
