package com.smallnine.apiserver.interceptor;

import com.smallnine.apiserver.logging.LogContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 日誌攔截器
 * 自動記錄所有API請求和響應信息
 */
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    
    private static final String START_TIME = "startTime";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME, startTime);

        // 獲取客戶端IP
        String clientIp = getClientIp(request);

        // 初始化請求上下文（設置 traceId 和 MDC）
        String traceId = LogContext.initRequest(request.getMethod(), request.getRequestURI(), clientIp);

        // 檢查是否有外部傳入的 traceId（用於分布式追蹤）
        String externalTraceId = request.getHeader("X-Trace-Id");
        if (externalTraceId != null && !externalTraceId.isEmpty()) {
            LogContext.setTraceId(externalTraceId);
        }

        // 記錄請求開始
        log.info("action=REQUEST_START method={} uri={} client_ip={} user_agent=\"{}\"",
                request.getMethod(),
                request.getRequestURI(),
                clientIp,
                request.getHeader("User-Agent"));

        // 記錄請求參數（DEBUG 級別）
        if (log.isDebugEnabled()) {
            StringBuilder params = new StringBuilder();
            request.getParameterMap().forEach((key, values) -> {
                params.append(key).append("=").append(String.join(",", values)).append(" ");
            });
            if (!params.isEmpty()) {
                log.debug("request_params={}", params.toString().trim());
            }
        }

        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 可以在這裡記錄處理完成但還沒返回響應的信息
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        try {
            Long startTime = (Long) request.getAttribute(START_TIME);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;

                // 記錄請求完成（結構化格式）
                String status = ex != null ? "FAILED" : "SUCCESS";
                log.info("action=REQUEST_END method={} uri={} status={} http_code={} duration_ms={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        response.getStatus(),
                        duration);

                // 記錄異常信息
                if (ex != null) {
                    log.error("action=REQUEST_ERROR method={} uri={} error=\"{}\"",
                            request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
                }

                // 性能監控：慢請求警告
                if (duration > 2000) {
                    log.warn("action=SLOW_REQUEST method={} uri={} duration_ms={}",
                            request.getMethod(), request.getRequestURI(), duration);
                }
            }
        } finally {
            // 清除 MDC 信息
            LogContext.clear();
        }
    }
    
    /**
     * 獲取真實的客戶端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}