package com.smallnine.apiserver.utils;

public final class SqlSecurityUtil {

    private SqlSecurityUtil() {
        // Utility class
    }

    /**
     * 轉義LIKE查詢中的特殊字符，防止通配符注入
     * @param input 用戶輸入
     * @return 轉義後的字符串
     */
    public static String escapeLikePattern(String input) {
        if (input == null) {
            return null;
        }
        
        return input
                .replace("\\", "\\\\")  // 轉義反斜線
                .replace("%", "\\%")    // 轉義百分號
                .replace("_", "\\_");   // 轉義下劃線
    }
}