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
    
    /**
     * 清理和驗證輸入字符串
     * @param input 用戶輸入
     * @param maxLength 最大長度
     * @return 清理後的字符串
     */
    public static String sanitizeInput(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        
        // 移除潛在危險字符
        String cleaned = input.trim()
                .replaceAll("[<>\"'\\\\]", "");  // 移除HTML/SQL特殊字符
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned.isEmpty() ? null : cleaned;
    }
}