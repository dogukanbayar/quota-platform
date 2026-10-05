package com.saasplatform.quota.service;

import com.saasplatform.quota.entity.Subscription;
import com.saasplatform.quota.entity.SubscriptionStatus;
import com.saasplatform.quota.exception.SubscriptionExpiredException;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Central place for the "an expired subscription cannot be used" rule. */
@Component
public class SubscriptionGuard {

    public void assertActive(Subscription subscription, Instant now) {
        if (subscription.effectiveStatus(now) == SubscriptionStatus.EXPIRED) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            throw new SubscriptionExpiredException(subscription.getEndDate());
        }
    }
}
