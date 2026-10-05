package com.saasplatform.quota.exception;

import org.springframework.http.HttpStatus;

/** Base type for all domain errors; carries the HTTP status and a stable machine-readable code. */
public class BusinessException extends RuntimeException {

    private final transient HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
