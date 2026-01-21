package com.smallnine.apiserver.logging.aspect;

import com.smallnine.apiserver.logging.BusinessLogger;
import com.smallnine.apiserver.logging.annotation.LogPerformance;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 性能日誌切面
 * 自動記錄帶有 @LogPerformance 註解的方法執行時間
 */
@Aspect
@Component
@Order(2)
public class PerformanceLogAspect {

    private final BusinessLogger businessLogger;

    public PerformanceLogAspect(BusinessLogger businessLogger) {
        this.businessLogger = businessLogger;
    }

    @Around("@annotation(logPerformance)")
    public Object logPerformance(ProceedingJoinPoint joinPoint, LogPerformance logPerformance) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 決定是否記錄
            if (logPerformance.alwaysLog() || duration > logPerformance.threshold()) {
                String operation = logPerformance.operation();
                if (operation.isEmpty()) {
                    // 使用方法名作為操作名
                    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                    operation = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
                }
                businessLogger.logPerformance(operation, duration);
            }
        }
    }
}
