package com.saasplatform.quota.exception;

import org.springframework.http.HttpStatus;

public class InvalidOperationException extends BusinessException {

    public InvalidOperationException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_OPERATION", message);
    }
}
