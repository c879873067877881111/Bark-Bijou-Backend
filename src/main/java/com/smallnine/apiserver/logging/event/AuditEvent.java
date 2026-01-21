package com.smallnine.apiserver.logging.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smallnine.apiserver.logging.constants.AuditAction;
import com.smallnine.apiserver.logging.constants.AuditResult;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 審計日誌事件
 * 結構化格式，便於 ELK/Splunk 解析和合規審計
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEvent {

    private final Instant timestamp;
    private final String traceId;
    private final AuditAction action;
    private final AuditResult result;
    private final String userId;
    private final String username;
    private final String clientIp;
    private final String resource;
    private final String resourceId;
    private final String description;
    private final Map<String, Object> details;
    private final Long duration;

    private AuditEvent(Builder builder) {
        this.timestamp = Instant.now();
        this.traceId = builder.traceId;
        this.action = builder.action;
        this.result = builder.result;
        this.userId = builder.userId;
        this.username = builder.username;
        this.clientIp = builder.clientIp;
        this.resource = builder.resource;
        this.resourceId = builder.resourceId;
        this.description = builder.description;
        this.details = builder.details;
        this.duration = builder.duration;
    }

    public static Builder builder(AuditAction action) {
        return new Builder(action);
    }

    /**
     * 轉換為結構化日誌字符串 (key=value 格式)
     */
    public String toLogString() {
        StringBuilder sb = new StringBuilder();
        sb.append("audit_event=true");
        sb.append(" action=").append(action.name());
        sb.append(" result=").append(result.name());

        if (userId != null) sb.append(" user_id=").append(userId);
        if (username != null) sb.append(" username=").append(username);
        if (clientIp != null) sb.append(" client_ip=").append(clientIp);
        if (resource != null) sb.append(" resource=").append(resource);
        if (resourceId != null) sb.append(" resource_id=").append(resourceId);
        if (description != null) sb.append(" desc=\"").append(description).append("\"");
        if (duration != null) sb.append(" duration_ms=").append(duration);
        if (traceId != null) sb.append(" trace_id=").append(traceId);

        if (details != null && !details.isEmpty()) {
            details.forEach((k, v) -> sb.append(" ").append(k).append("=").append(v));
        }

        return sb.toString();
    }

    // Getters
    public Instant getTimestamp() { return timestamp; }
    public String getTraceId() { return traceId; }
    public AuditAction getAction() { return action; }
    public AuditResult getResult() { return result; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getClientIp() { return clientIp; }
    public String getResource() { return resource; }
    public String getResourceId() { return resourceId; }
    public String getDescription() { return description; }
    public Map<String, Object> getDetails() { return details; }
    public Long getDuration() { return duration; }

    public static class Builder {
        private final AuditAction action;
        private AuditResult result = AuditResult.SUCCESS;
        private String traceId;
        private String userId;
        private String username;
        private String clientIp;
        private String resource;
        private String resourceId;
        private String description;
        private Map<String, Object> details;
        private Long duration;

        private Builder(AuditAction action) {
            this.action = action;
        }

        public Builder result(AuditResult result) {
            this.result = result;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder resourceId(Long resourceId) {
            this.resourceId = resourceId != null ? resourceId.toString() : null;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder duration(Long duration) {
            this.duration = duration;
            return this;
        }

        public Builder detail(String key, Object value) {
            if (this.details == null) {
                this.details = new HashMap<>();
            }
            this.details.put(key, value);
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public AuditEvent build() {
            return new AuditEvent(this);
        }
    }
}
