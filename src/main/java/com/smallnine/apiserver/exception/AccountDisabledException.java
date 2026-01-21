package com.smallnine.apiserver.exception;

/**
 * 帳號停用異常
 */
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException(String message) {
        super(message);
    }

    public AccountDisabledException() {
        super("帳號已被停用或信箱未驗證");
    }
}
