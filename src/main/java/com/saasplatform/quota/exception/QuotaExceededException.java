package com.saasplatform.quota.exception;

import org.springframework.http.HttpStatus;

public class QuotaExceededException extends BusinessException {

    public QuotaExceededException(long used, int requested, int limit) {
        super(HttpStatus.TOO_MANY_REQUESTS, "QUOTA_EXCEEDED",
                "Monthly quota exceeded: used " + used + ", requested " + requested + ", limit " + limit);
    }
}
