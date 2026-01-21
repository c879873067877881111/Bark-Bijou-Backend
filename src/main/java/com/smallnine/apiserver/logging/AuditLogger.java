package com.smallnine.apiserver.logging;

import com.smallnine.apiserver.logging.constants.AuditAction;
import com.smallnine.apiserver.logging.constants.AuditResult;
import com.smallnine.apiserver.logging.constants.LogConstants;
import com.smallnine.apiserver.logging.event.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 審計日誌記錄器
 * 專門記錄需要合規審計的操作
 *
 * 使用獨立的 Logger，輸出到獨立的日誌文件
 */
@Component
public class AuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger(LogConstants.AUDIT_LOGGER);
    private static final Logger securityLog = LoggerFactory.getLogger(LogConstants.SECURITY_LOGGER);

    /**
     * 記錄審計事件
     */
    public void log(AuditEvent event) {
        auditLog.info(event.toLogString());
    }

    /**
     * 快速記錄成功的審計事件
     */
    public void logSuccess(AuditAction action, String resource, String resourceId) {
        AuditEvent event = AuditEvent.builder(action)
                .result(AuditResult.SUCCESS)
                .traceId(LogContext.getTraceId())
                .userId(LogContext.getUserId())
                .username(LogContext.getUsername())
                .clientIp(LogContext.getClientIp())
                .resource(resource)
                .resourceId(resourceId)
                .build();
        auditLog.info(event.toLogString());
    }

    /**
     * 快速記錄失敗的審計事件
     */
    public void logFailure(AuditAction action, String resource, String reason) {
        AuditEvent event = AuditEvent.builder(action)
                .result(AuditResult.FAILURE)
                .traceId(LogContext.getTraceId())
                .userId(LogContext.getUserId())
                .username(LogContext.getUsername())
                .clientIp(LogContext.getClientIp())
                .resource(resource)
                .description(reason)
                .build();
        auditLog.warn(event.toLogString());
    }

    // ===== 安全事件快捷方法 =====

    /**
     * 記錄登入成功
     */
    public void logLoginSuccess(String userId, String username) {
        AuditEvent event = AuditEvent.builder(AuditAction.LOGIN)
                .result(AuditResult.SUCCESS)
                .traceId(LogContext.getTraceId())
                .userId(userId)
                .username(username)
                .clientIp(LogContext.getClientIp())
                .build();
        securityLog.info(event.toLogString());
    }

    /**
     * 記錄登入失敗
     */
    public void logLoginFailure(String username, String reason) {
        AuditEvent event = AuditEvent.builder(AuditAction.LOGIN_FAILED)
                .result(AuditResult.FAILURE)
                .traceId(LogContext.getTraceId())
                .username(username)
                .clientIp(LogContext.getClientIp())
                .description(reason)
                .build();
        securityLog.warn(event.toLogString());
    }

    /**
     * 記錄登出
     */
    public void logLogout(String userId, String username) {
        AuditEvent event = AuditEvent.builder(AuditAction.LOGOUT)
                .result(AuditResult.SUCCESS)
                .traceId(LogContext.getTraceId())
                .userId(userId)
                .username(username)
                .clientIp(LogContext.getClientIp())
                .build();
        securityLog.info(event.toLogString());
    }

    /**
     * 記錄權限拒絕
     */
    public void logAccessDenied(String resource, String reason) {
        AuditEvent event = AuditEvent.builder(AuditAction.READ)
                .result(AuditResult.DENIED)
                .traceId(LogContext.getTraceId())
                .userId(LogContext.getUserId())
                .username(LogContext.getUsername())
                .clientIp(LogContext.getClientIp())
                .resource(resource)
                .description(reason)
                .build();
        securityLog.warn(event.toLogString());
    }

    // ===== CRUD 操作審計 =====

    /**
     * 記錄創建操作
     */
    public void logCreate(String entity, Long entityId) {
        logSuccess(AuditAction.CREATE, entity, entityId != null ? entityId.toString() : null);
    }

    /**
     * 記錄更新操作
     */
    public void logUpdate(String entity, Long entityId) {
        logSuccess(AuditAction.UPDATE, entity, entityId != null ? entityId.toString() : null);
    }

    /**
     * 記錄刪除操作
     */
    public void logDelete(String entity, Long entityId) {
        logSuccess(AuditAction.DELETE, entity, entityId != null ? entityId.toString() : null);
    }
}
