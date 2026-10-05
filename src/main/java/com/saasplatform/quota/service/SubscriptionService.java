package com.saasplatform.quota.service;

import com.saasplatform.quota.dto.SubscriptionResponse;
import com.saasplatform.quota.entity.Plan;
import com.saasplatform.quota.entity.PlanType;
import com.saasplatform.quota.entity.Subscription;
import com.saasplatform.quota.entity.SubscriptionStatus;
import com.saasplatform.quota.exception.InvalidOperationException;
import com.saasplatform.quota.exception.PlanDowngradeNotAllowedException;
import com.saasplatform.quota.exception.ResourceNotFoundException;
import com.saasplatform.quota.exception.SubscriptionExpiredException;
import com.saasplatform.quota.mapper.SubscriptionMapper;
import com.saasplatform.quota.repository.PlanRepository;
import com.saasplatform.quota.repository.SubscriptionRepository;
import com.saasplatform.quota.repository.UsageLogRepository;
import com.saasplatform.quota.util.SubscriptionPeriods;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UsageLogRepository usageLogRepository;
    private final PlanRepository planRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionGuard guard;
    private final Clock clock;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, UsageLogRepository usageLogRepository,
                               PlanRepository planRepository, SubscriptionMapper subscriptionMapper,
                               SubscriptionGuard guard, Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.usageLogRepository = usageLogRepository;
        this.planRepository = planRepository;
        this.subscriptionMapper = subscriptionMapper;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getByUserId(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription for user", userId));
        return toResponse(subscription, clock.instant());
    }

    /**
     * Upgrade or downgrade. A downgrade is rejected when this month's usage already exceeds
     * the monthly limit of the target plan.
     */
    @Transactional(noRollbackFor = SubscriptionExpiredException.class)
    public SubscriptionResponse changePlan(Long userId, PlanType target) {
        Subscription subscription = lockByUserId(userId);
        Instant now = clock.instant();
        guard.assertActive(subscription, now);

        Plan current = subscription.getPlan();
        if (current.getType() == target) {
            throw new InvalidOperationException("Subscription is already on the " + target + " plan");
        }
        Plan targetPlan = planRepository.findByType(target)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", target));

        String period = SubscriptionPeriods.currentPeriod(clock);
        long used = usageLogRepository.sumQuotaUsed(subscription.getId(), period);
        boolean downgrade = targetPlan.getMonthlyLimit() < current.getMonthlyLimit();
        if (downgrade && used > targetPlan.getMonthlyLimit()) {
            throw new PlanDowngradeNotAllowedException(current.getType(), target, used, targetPlan.getMonthlyLimit());
        }
        subscription.setPlan(targetPlan);
        return subscriptionMapper.toResponse(subscription, subscription.effectiveStatus(now), used, period);
    }

    /** Starts a fresh one month term. Only allowed once the previous term has expired. */
    @Transactional
    public SubscriptionResponse renew(Long userId) {
        Subscription subscription = lockByUserId(userId);
        Instant now = clock.instant();
        if (subscription.effectiveStatus(now) == SubscriptionStatus.ACTIVE) {
            throw new InvalidOperationException("Subscription is still active and cannot be renewed yet");
        }
        subscription.renew(now, SubscriptionPeriods.endFrom(now));
        return toResponse(subscription, now);
    }

    /** Back-office / demo operation: terminates the current term immediately. */
    @Transactional
    public SubscriptionResponse expire(Long userId) {
        Subscription subscription = lockByUserId(userId);
        Instant now = clock.instant();
        subscription.expire(now);
        return toResponse(subscription, now);
    }

    private Subscription lockByUserId(Long userId) {
        return subscriptionRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription for user", userId));
    }

    private SubscriptionResponse toResponse(Subscription subscription, Instant now) {
        String period = SubscriptionPeriods.currentPeriod(clock);
        long used = usageLogRepository.sumQuotaUsed(subscription.getId(), period);
        return subscriptionMapper.toResponse(subscription, subscription.effectiveStatus(now), used, period);
    }
}
