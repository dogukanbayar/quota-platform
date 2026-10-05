package com.saasplatform.quota.service;

import com.saasplatform.quota.dto.DailyUsageResponse;
import com.saasplatform.quota.dto.PageResponse;
import com.saasplatform.quota.dto.UsageLogResponse;
import com.saasplatform.quota.dto.UsageRequest;
import com.saasplatform.quota.dto.UsageResponse;
import com.saasplatform.quota.entity.Subscription;
import com.saasplatform.quota.entity.UsageLog;
import com.saasplatform.quota.exception.QuotaExceededException;
import com.saasplatform.quota.exception.ResourceNotFoundException;
import com.saasplatform.quota.exception.SubscriptionExpiredException;
import com.saasplatform.quota.mapper.UsageMapper;
import com.saasplatform.quota.repository.SubscriptionRepository;
import com.saasplatform.quota.repository.UsageLogRepository;
import com.saasplatform.quota.util.SubscriptionPeriods;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageService {

    private static final String SUBSCRIPTION_RESOURCE = "Subscription for user";

    private final SubscriptionRepository subscriptionRepository;
    private final UsageLogRepository usageLogRepository;
    private final UsageMapper usageMapper;
    private final SubscriptionGuard guard;
    private final Clock clock;

    public UsageService(SubscriptionRepository subscriptionRepository, UsageLogRepository usageLogRepository,
                        UsageMapper usageMapper, SubscriptionGuard guard, Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.usageLogRepository = usageLogRepository;
        this.usageMapper = usageMapper;
        this.guard = guard;
        this.clock = clock;
    }

    /**
     * Records one usage event and deducts it from the monthly quota.
     * The subscription row is locked so concurrent calls cannot overshoot the limit.
     * The "expired" flag is persisted even though the call itself is rejected.
     */
    @Transactional(noRollbackFor = SubscriptionExpiredException.class)
    public UsageResponse consume(Long userId, UsageRequest request) {
        Subscription subscription = subscriptionRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(SUBSCRIPTION_RESOURCE, userId));
        Instant now = clock.instant();
        guard.assertActive(subscription, now);

        String period = SubscriptionPeriods.currentPeriod(clock);
        int units = request.unitsOrDefault();
        int limit = subscription.getPlan().getMonthlyLimit();
        long used = usageLogRepository.sumQuotaUsed(subscription.getId(), period);
        if (used + units > limit) {
            throw new QuotaExceededException(used, units, limit);
        }

        UsageLog saved = usageLogRepository.save(new UsageLog(subscription, request.operation().trim(), units, period, now));
        return usageMapper.toUsageResponse(saved, used + units, limit);
    }

    /** Quota units consumed per UTC day in the current billing month; days without usage are omitted. */
    @Transactional(readOnly = true)
    public List<DailyUsageResponse> dailyUsage(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(SUBSCRIPTION_RESOURCE, userId));
        String period = SubscriptionPeriods.currentPeriod(clock);
        Map<LocalDate, Long> perDay = usageLogRepository
                .findBySubscriptionIdAndUsagePeriod(subscription.getId(), period).stream()
                .collect(Collectors.groupingBy(log -> log.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                        TreeMap::new, Collectors.summingLong(UsageLog::getQuotaUsed)));
        return perDay.entrySet().stream()
                .map(entry -> new DailyUsageResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<UsageLogResponse> history(Long userId, int page, int size) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(SUBSCRIPTION_RESOURCE, userId));
        Page<UsageLog> result = usageLogRepository
                .findBySubscriptionIdOrderByCreatedAtDescIdDesc(subscription.getId(), PageRequest.of(page, size));
        return new PageResponse<>(result.map(usageMapper::toLogResponse).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
