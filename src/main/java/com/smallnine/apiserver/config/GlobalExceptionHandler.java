package com.smallnine.apiserver.config;

import com.smallnine.apiserver.constants.enums.ResponseCode;
import com.smallnine.apiserver.dto.ApiResponse;
import com.smallnine.apiserver.exception.AccountDisabledException;
import com.smallnine.apiserver.exception.BusinessException;
import com.smallnine.apiserver.exception.DuplicateResourceException;
import com.smallnine.apiserver.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 業務異常處理
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, WebRequest request) {

        log.warn("業務異常: code={}, message={}, 請求: {}", ex.getCode(), ex.getMessage(), request.getDescription(false));

        ApiResponse<Void> response = ApiResponse.error(ex.getCode(), ex.getMessage());
        HttpStatus status = ResponseCode.fromCode(ex.getCode()).getHttpStatus();
        return new ResponseEntity<>(response, status);
    }

    /**
     * 資源不存在異常處理
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("資源不存在: {}, 請求: {}", ex.getMessage(), request.getDescription(false));

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.NOT_FOUND.getCode(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * 資源重複異常處理
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {

        log.warn("資源重複: {}, 請求: {}", ex.getMessage(), request.getDescription(false));

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.BAD_REQUEST.getCode(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    /**
     * 參數驗證異常處理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        log.warn("參數驗證失敗: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse<Map<String, String>> response = ApiResponse.error(
                ResponseCode.BAD_REQUEST.getCode(), "參數驗證失敗");
        response.setData(errors);
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 資料庫約束違反異常處理
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        
        log.error("數據庫約束違反: {}, 請求: {}", ex.getMessage(), request.getDescription(false));
        
        String message = "數據操作失敗,請檢查數據完整性";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("unique")) {
                message = "數據已存在,不能重複";
            } else if (ex.getMessage().contains("foreign key")) {
                message = "存在關聯數據,無法刪除";
            }
        }
        
        ApiResponse<Void> response = ApiResponse.error(ResponseCode.BAD_REQUEST.getCode(), message);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    /**
     * 帳號停用異常處理
     */
    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountDisabledException(
            AccountDisabledException ex, WebRequest request) {

        log.warn("帳號停用: {}, 請求: {}", ex.getMessage(), request.getDescription(false));

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.FORBIDDEN.getCode(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * 認證異常處理
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        log.warn("認證失敗: {}, 請求: {}", ex.getMessage(), request.getDescription(false));

        ApiResponse<Void> response = ApiResponse.error(ResponseCode.UNAUTHORIZED.getCode(), "用戶名或密碼錯誤");
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * 授權異常處理
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        log.warn("授權失敗: {}, 請求: {}", ex.getMessage(), request.getDescription(false));
        
        ApiResponse<Void> response = ApiResponse.error(ResponseCode.FORBIDDEN);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    
    /**
     * 運行時異常處理
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        log.error("運行時異常: {}, 請求: {}", ex.getMessage(), request.getDescription(false), ex);
        
        ApiResponse<Void> response = ApiResponse.error(
                ResponseCode.INTERNAL_SERVER_ERROR.getCode(), 
                "系統處理異常,請稍後重試");
                
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 其他所有異常處理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(
            Exception ex, WebRequest request) {
        
        log.error("系統異常: {}, 請求: {}", ex.getMessage(), request.getDescription(false), ex);
        
        ApiResponse<Void> response = ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}