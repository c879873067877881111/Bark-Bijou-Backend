package com.smallnine.apiserver.logging.aspect;

import com.smallnine.apiserver.logging.AuditLogger;
import com.smallnine.apiserver.logging.LogContext;
import com.smallnine.apiserver.logging.annotation.Auditable;
import com.smallnine.apiserver.logging.constants.AuditResult;
import com.smallnine.apiserver.logging.event.AuditEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * 審計日誌切面
 * 自動記錄帶有 @Auditable 註解的方法
 */
@Aspect
@Component
@Order(1)
public class AuditLogAspect {

    private final AuditLogger auditLogger;
    private final ExpressionParser spelParser = new SpelExpressionParser();

    public AuditLogAspect(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            recordAuditLog(joinPoint, auditable, result, error, duration);
        }
    }

    private void recordAuditLog(ProceedingJoinPoint joinPoint, Auditable auditable,
                                Object result, Throwable error, long duration) {
        try {
            AuditEvent.Builder builder = AuditEvent.builder(auditable.action())
                    .traceId(LogContext.getTraceId())
                    .userId(LogContext.getUserId())
                    .username(LogContext.getUsername())
                    .clientIp(LogContext.getClientIp())
                    .resource(auditable.resource())
                    .duration(duration);

            // 設置結果狀態
            if (error != null) {
                builder.result(AuditResult.FAILURE)
                        .description(error.getMessage());
            } else {
                builder.result(AuditResult.SUCCESS);
            }

            // 解析 resourceId（使用 SpEL）
            String resourceIdExpr = auditable.resourceId();
            if (!resourceIdExpr.isEmpty()) {
                String resourceId = evaluateSpelExpression(joinPoint, result, resourceIdExpr);
                builder.resourceId(resourceId);
            }

            // 添加描述
            if (!auditable.description().isEmpty()) {
                builder.description(auditable.description());
            }

            // 記錄參數（如果啟用）
            if (auditable.logParams()) {
                Map<String, Object> params = extractMethodParams(joinPoint);
                builder.details(params);
            }

            // 記錄返回值（如果啟用且成功）
            if (auditable.logResult() && result != null && error == null) {
                builder.detail("result_type", result.getClass().getSimpleName());
            }

            auditLogger.log(builder.build());
        } catch (Exception e) {
            // 審計日誌失敗不應影響業務
        }
    }

    private String evaluateSpelExpression(ProceedingJoinPoint joinPoint, Object result, String expression) {
        try {
            EvaluationContext context = createEvaluationContext(joinPoint, result);
            Object value = spelParser.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 添加方法參數
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 添加返回值
        context.setVariable("result", result);

        return context;
    }

    private Map<String, Object> extractMethodParams(ProceedingJoinPoint joinPoint) {
        Map<String, Object> params = new HashMap<>();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            Object paramValue = args[i];
            // 避免記錄敏感信息
            if (!isSensitiveParam(paramName)) {
                params.put(paramName, paramValue != null ? paramValue.toString() : null);
            }
        }

        return params;
    }

    private boolean isSensitiveParam(String paramName) {
        String lower = paramName.toLowerCase();
        return lower.contains("password") ||
                lower.contains("secret") ||
                lower.contains("token") ||
                lower.contains("credential");
    }
}
