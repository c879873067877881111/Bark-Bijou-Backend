package com.smallnine.apiserver.logging.constants;

/**
 * 審計結果狀態
 */
public enum AuditResult {
    SUCCESS("成功"),
    FAILURE("失敗"),
    DENIED("拒絕"),
    ERROR("錯誤");

    private final String description;

    AuditResult(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
