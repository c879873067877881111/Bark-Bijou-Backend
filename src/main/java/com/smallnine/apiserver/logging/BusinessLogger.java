package com.smallnine.apiserver.logging;

import com.smallnine.apiserver.logging.constants.LogConstants;
import com.smallnine.apiserver.logging.event.BusinessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 業務日誌記錄器
 * 記錄一般業務操作（非審計級別）
 */
@Component
public class BusinessLogger {

    private static final Logger bizLog = LoggerFactory.getLogger(LogConstants.BUSINESS_LOGGER);
    private static final Logger perfLog = LoggerFactory.getLogger(LogConstants.PERFORMANCE_LOGGER);

    /**
     * 記錄業務事件
     */
    public void log(BusinessEvent event) {
        if (event.isSuccess()) {
            bizLog.info(event.toLogString());
        } else {
            bizLog.warn(event.toLogString());
        }
    }

    /**
     * 快速記錄成功操作
     */
    public void logSuccess(String operation, String entity, Long entityId) {
        BusinessEvent event = BusinessEvent.builder(operation)
                .entity(entity)
                .entityId(entityId)
                .success(true)
                .build();
        bizLog.info(event.toLogString());
    }

    /**
     * 快速記錄失敗操作
     */
    public void logFailure(String operation, String entity, String reason) {
        BusinessEvent event = BusinessEvent.builder(operation)
                .entity(entity)
                .success(false)
                .message(reason)
                .build();
        bizLog.warn(event.toLogString());
    }

    /**
     * 記錄性能信息
     */
    public void logPerformance(String operation, long durationMs) {
        if (durationMs > LogConstants.VERY_SLOW_THRESHOLD) {
            perfLog.error("action={} duration_ms={} level=CRITICAL", operation, durationMs);
        } else if (durationMs > LogConstants.SLOW_API_THRESHOLD) {
            perfLog.warn("action={} duration_ms={} level=SLOW", operation, durationMs);
        } else if (durationMs > LogConstants.SLOW_QUERY_THRESHOLD) {
            perfLog.info("action={} duration_ms={} level=MODERATE", operation, durationMs);
        } else {
            perfLog.debug("action={} duration_ms={} level=NORMAL", operation, durationMs);
        }
    }

    /**
     * 創建事件 Builder（鏈式調用）
     */
    public BusinessEventBuilder operation(String operation) {
        return new BusinessEventBuilder(operation);
    }

    /**
     * 內部 Builder 類，提供流暢的 API
     */
    public class BusinessEventBuilder {
        private final BusinessEvent.Builder builder;

        private BusinessEventBuilder(String operation) {
            this.builder = BusinessEvent.builder(operation);
        }

        public BusinessEventBuilder entity(String entity) {
            builder.entity(entity);
            return this;
        }

        public BusinessEventBuilder entityId(Long entityId) {
            builder.entityId(entityId);
            return this;
        }

        public BusinessEventBuilder with(String key, Object value) {
            builder.with(key, value);
            return this;
        }

        public BusinessEventBuilder duration(long durationMs) {
            builder.duration(durationMs);
            return this;
        }

        public BusinessEventBuilder message(String message) {
            builder.message(message);
            return this;
        }

        public void success() {
            builder.success(true);
            log(builder.build());
        }

        public void failure(String reason) {
            builder.success(false).message(reason);
            log(builder.build());
        }
    }
}
