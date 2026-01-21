package com.smallnine.apiserver.logging.constants;

/**
 * 審計操作類型
 * 用於分類和過濾審計日誌
 */
public enum AuditAction {
    // 認證相關
    LOGIN("登入"),
    LOGOUT("登出"),
    LOGIN_FAILED("登入失敗"),
    TOKEN_REFRESH("令牌刷新"),
    PASSWORD_CHANGE("密碼變更"),

    // 數據操作
    CREATE("創建"),
    READ("讀取"),
    UPDATE("更新"),
    DELETE("刪除"),
    EXPORT("導出"),
    IMPORT("導入"),

    // 權限相關
    PERMISSION_GRANT("授予權限"),
    PERMISSION_REVOKE("撤銷權限"),
    ROLE_ASSIGN("分配角色"),

    // 系統操作
    CONFIG_CHANGE("配置變更"),
    SYSTEM_STARTUP("系統啟動"),
    SYSTEM_SHUTDOWN("系統關閉");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
