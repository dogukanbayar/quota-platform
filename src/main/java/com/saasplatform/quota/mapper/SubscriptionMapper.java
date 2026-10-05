package com.saasplatform.quota.mapper;

import com.saasplatform.quota.dto.SubscriptionResponse;
import com.saasplatform.quota.entity.Subscription;
import com.saasplatform.quota.entity.SubscriptionStatus;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    public SubscriptionResponse toResponse(Subscription sub, SubscriptionStatus effectiveStatus, long used, String period) {
        int limit = sub.getPlan().getMonthlyLimit();
        return new SubscriptionResponse(sub.getId(), sub.getUser().getId(), sub.getPlan().getType(), effectiveStatus,
                sub.getStartDate(), sub.getEndDate(), limit, used, Math.max(0L, limit - used), period);
    }
}
