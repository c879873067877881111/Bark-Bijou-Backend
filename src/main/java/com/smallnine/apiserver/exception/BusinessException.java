package com.smallnine.apiserver.exception;

import com.smallnine.apiserver.constants.enums.ResponseCode;

public class BusinessException extends RuntimeException {
    private final int code;
    
    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
    }
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
    }
    
    public int getCode() {
        return code;
    }
}