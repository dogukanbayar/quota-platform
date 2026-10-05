package com.saasplatform.quota.mapper;

import com.saasplatform.quota.dto.UsageLogResponse;
import com.saasplatform.quota.dto.UsageResponse;
import com.saasplatform.quota.entity.UsageLog;
import org.springframework.stereotype.Component;

@Component
public class UsageMapper {

    public UsageLogResponse toLogResponse(UsageLog log) {
        return new UsageLogResponse(log.getId(), log.getOperation(), log.getQuotaUsed(),
                log.getUsagePeriod(), log.getCreatedAt());
    }

    public UsageResponse toUsageResponse(UsageLog log, long usedThisMonth, int monthlyLimit) {
        return new UsageResponse(log.getId(), log.getOperation(), log.getQuotaUsed(), usedThisMonth,
                Math.max(0L, monthlyLimit - usedThisMonth), monthlyLimit, log.getCreatedAt());
    }
}
