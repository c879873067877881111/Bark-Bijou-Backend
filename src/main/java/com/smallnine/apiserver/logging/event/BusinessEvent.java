package com.smallnine.apiserver.logging.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 業務操作日誌事件
 * 記錄一般業務操作（非審計級別）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BusinessEvent {

    private final Instant timestamp;
    private final String operation;
    private final String entity;
    private final String entityId;
    private final boolean success;
    private final String message;
    private final Long duration;
    private final Map<String, Object> context;

    private BusinessEvent(Builder builder) {
        this.timestamp = Instant.now();
        this.operation = builder.operation;
        this.entity = builder.entity;
        this.entityId = builder.entityId;
        this.success = builder.success;
        this.message = builder.message;
        this.duration = builder.duration;
        this.context = builder.context;
    }

    public static Builder builder(String operation) {
        return new Builder(operation);
    }

    /**
     * 轉換為結構化日誌字符串
     */
    public String toLogString() {
        StringBuilder sb = new StringBuilder();
        sb.append("action=").append(operation);
        if (entity != null) sb.append(" entity=").append(entity);
        if (entityId != null) sb.append(" id=").append(entityId);
        sb.append(" success=").append(success);
        if (message != null) sb.append(" msg=\"").append(message).append("\"");
        if (duration != null) sb.append(" duration_ms=").append(duration);

        if (context != null && !context.isEmpty()) {
            context.forEach((k, v) -> {
                if (v != null) {
                    sb.append(" ").append(k).append("=").append(v);
                }
            });
        }

        return sb.toString();
    }

    // Getters
    public Instant getTimestamp() { return timestamp; }
    public String getOperation() { return operation; }
    public String getEntity() { return entity; }
    public String getEntityId() { return entityId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Long getDuration() { return duration; }
    public Map<String, Object> getContext() { return context; }

    public static class Builder {
        private final String operation;
        private String entity;
        private String entityId;
        private boolean success = true;
        private String message;
        private Long duration;
        private Map<String, Object> context;

        private Builder(String operation) {
            this.operation = operation;
        }

        public Builder entity(String entity) {
            this.entity = entity;
            return this;
        }

        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder entityId(Long entityId) {
            this.entityId = entityId != null ? entityId.toString() : null;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder failed() {
            this.success = false;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder duration(Long duration) {
            this.duration = duration;
            return this;
        }

        public Builder with(String key, Object value) {
            if (this.context == null) {
                this.context = new HashMap<>();
            }
            this.context.put(key, value);
            return this;
        }

        public BusinessEvent build() {
            return new BusinessEvent(this);
        }
    }
}
