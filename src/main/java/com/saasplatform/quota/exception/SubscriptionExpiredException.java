package com.saasplatform.quota.exception;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public class SubscriptionExpiredException extends BusinessException {

    public SubscriptionExpiredException(Instant endDate) {
        super(HttpStatus.FORBIDDEN, "SUBSCRIPTION_EXPIRED", "Subscription expired at " + endDate + ". Please renew it.");
    }
}
