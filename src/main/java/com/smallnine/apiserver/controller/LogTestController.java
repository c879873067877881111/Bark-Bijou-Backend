package com.smallnine.apiserver.controller;

import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.utils.LogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 日誌測試控制器
 * 用於測試各種日誌功能
 */
@RestController
@RequestMapping("/api/log-test")
@Tag(name = "日誌測試", description = "測試各種日誌功能")
@Slf4j
public class LogTestController {
    
    @Operation(summary = "測試不同級別的日誌", description = "產生各種級別的日誌記錄")
    @GetMapping("/levels")
    public ResponseEntity<ApiResponse<String>> testLogLevels() {
        log.trace("這是TRACE級別日誌 - 最詳細的日誌信息");
        log.debug("這是DEBUG級別日誌 - 調試信息: {}", System.currentTimeMillis());
        log.info("這是INFO級別日誌 - 一般信息記錄");
        log.warn("這是WARN級別日誌 - 警告信息，需要注意");
        log.error("這是ERROR級別日誌 - 模擬錯誤信息");
        
        return ResponseEntity.ok(ApiResponse.success("已產生各級別日誌，請檢查控制台和日誌文件"));
    }
    
    @Operation(summary = "測試業務日誌", description = "測試業務操作的日誌記錄")
    @PostMapping("/business")
    public ResponseEntity<ApiResponse<String>> testBusinessLog(@RequestParam String operation) {
        LogUtil.logBusiness(log, operation, "這是一個測試的業務操作");
        
        // 模擬業務流程
        log.info("【業務流程】開始執行: {}", operation);
        log.debug("【業務流程】參數驗證通過");
        log.debug("【業務流程】調用外部服務");
        log.info("【業務流程】執行完成: {}", operation);
        
        return ResponseEntity.ok(ApiResponse.success("業務日誌記錄完成"));
    }
    
    @Operation(summary = "測試性能日誌", description = "模擬慢操作並記錄性能日誌")
    @GetMapping("/performance")
    public ResponseEntity<ApiResponse<String>> testPerformanceLog(@RequestParam(defaultValue = "1000") int delayMs) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("【性能測試】開始執行慢操作，預計耗時: {}ms", delayMs);
            
            // 模擬耗時操作
            TimeUnit.MILLISECONDS.sleep(delayMs);
            
            long duration = System.currentTimeMillis() - startTime;
            LogUtil.logPerformance(log, "模擬慢操作", duration, delayMs);
            
            return ResponseEntity.ok(ApiResponse.success(String.format("操作完成，實際耗時: %dms", duration)));
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【性能測試】操作被中斷", e);
            return ResponseEntity.ok(ApiResponse.success("操作被中斷"));
        }
    }
    
    @Operation(summary = "測試異常日誌", description = "模擬異常情況並記錄異常日誌")
    @GetMapping("/exception")
    public ResponseEntity<ApiResponse<String>> testExceptionLog(@RequestParam(defaultValue = "false") boolean throwError) {
        try {
            log.info("【異常測試】開始異常測試，是否拋出異常: {}", throwError);
            
            if (throwError) {
                throw new RuntimeException("這是一個測試異常");
            }
            
            log.info("【異常測試】正常執行完成");
            return ResponseEntity.ok(ApiResponse.success("正常執行，未發生異常"));
            
        } catch (Exception e) {
            LogUtil.logError(log, "異常測試操作", e);
            return ResponseEntity.ok(ApiResponse.success("異常已捕獲並記錄"));
        }
    }
    
    @Operation(summary = "測試安全日誌", description = "模擬安全相關事件的日誌記錄")
    @PostMapping("/security")
    public ResponseEntity<ApiResponse<String>> testSecurityLog(@RequestParam String event, @RequestParam String userId) {
        // 模擬獲取客戶端IP (在實際攔截器中已經設置)
        String clientIp = LogUtil.getTraceId(); // 這裡簡化處理
        
        LogUtil.logSecurity(log, event, userId, clientIp);
        
        // 記錄詳細的安全事件
        log.warn("【安全事件】用戶操作記錄 - 事件: {}, 用戶: {}, 時間: {}", 
                event, userId, System.currentTimeMillis());
        
        return ResponseEntity.ok(ApiResponse.success("安全事件已記錄"));
    }
    
    @Operation(summary = "測試MDC追蹤", description = "測試追蹤ID和MDC功能")
    @GetMapping("/mdc")
    public ResponseEntity<ApiResponse<String>> testMdc() {
        String traceId = LogUtil.getTraceId();
        
        log.info("當前追蹤ID: {}", traceId);
        log.info("模擬多步驟操作 - 步驟1");
        log.info("模擬多步驟操作 - 步驟2");
        log.info("模擬多步驟操作 - 步驟3");
        
        return ResponseEntity.ok(ApiResponse.success("追蹤ID: " + traceId));
    }
}