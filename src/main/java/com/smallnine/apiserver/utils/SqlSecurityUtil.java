package com.smallnine.apiserver.utils;

import org.springframework.stereotype.Component;

@Component
public class SqlSecurityUtil {
    
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
                .replaceAll("[<>\"'\\\\]", "")  // 移除HTML/SQL特殊字符
                .substring(0, Math.min(input.length(), maxLength));
                
        return cleaned.isEmpty() ? null : cleaned;
    }
    
    /**
     * 驗證搜索關鍵字
     * @param keyword 搜索關鍵字
     * @return 是否有效
     */
    public static boolean isValidSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        // 檢查最小長度
        if (keyword.trim().length() < 2) {
            return false;
        }
        
        // 檢查最大長度
        if (keyword.length() > 100) {
            return false;
        }
        
        // 檢查是否只包含通配符
        String cleaned = keyword.replaceAll("[%_\\s]", "");
        return !cleaned.isEmpty();
    }
}