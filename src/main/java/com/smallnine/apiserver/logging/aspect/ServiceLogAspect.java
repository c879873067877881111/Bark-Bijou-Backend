package com.smallnine.apiserver.logging.aspect;

import com.smallnine.apiserver.logging.BusinessLogger;
import com.smallnine.apiserver.logging.constants.LogConstants;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Service 層自動日誌切面
 * 自動記錄所有 Service 層的 public 方法（可配置開關）
 *
 * 僅在 DEBUG 模式下啟用詳細日誌
 */
@Aspect
@Component
@Order(10)
public class ServiceLogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogConstants.BUSINESS_LOGGER);
    private final BusinessLogger businessLogger;

    public ServiceLogAspect(BusinessLogger businessLogger) {
        this.businessLogger = businessLogger;
    }

    /**
     * 攔截所有 ServiceImpl 的 public 方法
     * 排除已經有 @Auditable 註解的方法（避免重複記錄）
     */
    @Around("execution(public * com.smallnine.apiserver.service.impl..*ServiceImpl.*(..)) " +
            "&& !@annotation(com.smallnine.apiserver.logging.annotation.Auditable)")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 只有在 DEBUG 級別啟用時才記錄詳細日誌
        if (!log.isDebugEnabled()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String operation = className + "." + methodName;

        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            success = false;
            errorMsg = t.getMessage();
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            if (success) {
                log.debug("action={} duration_ms={} status=SUCCESS", operation, duration);
            } else {
                log.debug("action={} duration_ms={} status=FAILED error=\"{}\"",
                        operation, duration, errorMsg);
            }

            // 記錄慢查詢
            if (duration > LogConstants.SLOW_QUERY_THRESHOLD) {
                businessLogger.logPerformance(operation, duration);
            }
        }
    }
}
