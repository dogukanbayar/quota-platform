package com.saasplatform.quota.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.saasplatform.quota.TestData;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UsageLogRepository usageLogRepository;
    @Mock
    private PlanRepository planRepository;

    private SubscriptionService service;
    private Plan free;
    private Plan pro;
    private Subscription proSubscription;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(subscriptionRepository, usageLogRepository, planRepository,
                new SubscriptionMapper(), new SubscriptionGuard(), TestData.clock());
        free = TestData.free();
        pro = TestData.pro();
        proSubscription = TestData.subscription(pro);
    }

    @Test
    void downgradeIsBlockedWhenUsageExceedsTargetLimit() {
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(proSubscription));
        when(planRepository.findByType(PlanType.FREE)).thenReturn(Optional.of(free));
        when(usageLogRepository.sumQuotaUsed(10L, TestData.PERIOD)).thenReturn(150L);

        assertThatThrownBy(() -> service.changePlan(1L, PlanType.FREE))
                .isInstanceOf(PlanDowngradeNotAllowedException.class)
                .hasMessageContaining("150");
        assertThat(proSubscription.getPlan()).isSameAs(pro);
    }

    @Test
    void downgradeIsAllowedWhenUsageEqualsTargetLimit() {
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(proSubscription));
        when(planRepository.findByType(PlanType.FREE)).thenReturn(Optional.of(free));
        when(usageLogRepository.sumQuotaUsed(10L, TestData.PERIOD)).thenReturn(100L);

        SubscriptionResponse response = service.changePlan(1L, PlanType.FREE);

        assertThat(response.planType()).isEqualTo(PlanType.FREE);
        assertThat(response.remaining()).isZero();
        assertThat(proSubscription.getPlan()).isSameAs(free);
    }

    @Test
    void upgradeIsAlwaysAllowedRegardlessOfUsage() {
        Subscription freeSubscription = TestData.subscription(free);
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(freeSubscription));
        when(planRepository.findByType(PlanType.PRO)).thenReturn(Optional.of(pro));
        when(usageLogRepository.sumQuotaUsed(10L, TestData.PERIOD)).thenReturn(100L);

        SubscriptionResponse response = service.changePlan(1L, PlanType.PRO);

        assertThat(response.planType()).isEqualTo(PlanType.PRO);
        assertThat(response.remaining()).isEqualTo(9_900);
    }

    @Test
    void changingToTheSamePlanIsRejected() {
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(proSubscription));

        assertThatThrownBy(() -> service.changePlan(1L, PlanType.PRO)).isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void changePlanFailsWhenTargetPlanIsMissing() {
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(proSubscription));
        when(planRepository.findByType(PlanType.ENTERPRISE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePlan(1L, PlanType.ENTERPRISE)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePlanIsRejectedForExpiredSubscription() {
        proSubscription.expire(TestData.NOW.minusSeconds(1));
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(proSubscription));

        assertThatThrownBy(() -> service.changePlan(1L, PlanType.FREE)).isInstanceOf(SubscriptionExpiredException.class);
    }

    @Test
    void renewReactivatesExpiredSubscription() {
        proSubscription.expire(TestData.NOW.minusSeconds(1));
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(proSubscription));
        when(usageLogRepository.sumQuotaUsed(10L, TestData.PERIOD)).thenReturn(0L);

        SubscriptionResponse response = service.renew(1L);

        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.startDate()).isEqualTo(TestData.NOW);
        assertThat(response.endDate()).isAfter(TestData.NOW);
    }

    @Test
    void renewIsRejectedWhileSubscriptionIsActive() {
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(proSubscription));

        assertThatThrownBy(() -> service.renew(1L)).isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void expireEndsTheCurrentTerm() {
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(proSubscription));
        when(usageLogRepository.sumQuotaUsed(10L, TestData.PERIOD)).thenReturn(5L);

        SubscriptionResponse response = service.expire(1L);

        assertThat(response.status()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(response.usedThisMonth()).isEqualTo(5);
    }

    @Test
    void getByUserIdReportsElapsedTermAsExpired() {
        Subscription elapsed = TestData.subscription(pro);
        elapsed.renew(TestData.NOW.minusSeconds(100), TestData.NOW.minusSeconds(1));
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(elapsed));
        when(usageLogRepository.sumQuotaUsed(10L, TestData.PERIOD)).thenReturn(0L);

        assertThat(service.getByUserId(1L).status()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    void getByUserIdFailsWhenMissing() {
        when(subscriptionRepository.findByUserId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByUserId(2L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
