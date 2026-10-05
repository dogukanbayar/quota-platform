package com.saasplatform.quota.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saasplatform.quota.TestData;
import com.saasplatform.quota.dto.DailyUsageResponse;
import java.time.Instant;
import java.time.LocalDate;
import com.saasplatform.quota.dto.PageResponse;
import com.saasplatform.quota.dto.UsageLogResponse;
import com.saasplatform.quota.dto.UsageRequest;
import com.saasplatform.quota.dto.UsageResponse;
import com.saasplatform.quota.entity.Subscription;
import com.saasplatform.quota.entity.SubscriptionStatus;
import com.saasplatform.quota.entity.UsageLog;
import com.saasplatform.quota.exception.QuotaExceededException;
import com.saasplatform.quota.exception.ResourceNotFoundException;
import com.saasplatform.quota.exception.SubscriptionExpiredException;
import com.saasplatform.quota.mapper.UsageMapper;
import com.saasplatform.quota.repository.SubscriptionRepository;
import com.saasplatform.quota.repository.UsageLogRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UsageLogRepository usageLogRepository;

    private UsageService service;
    private Subscription freeSubscription;

    @BeforeEach
    void setUp() {
        service = new UsageService(subscriptionRepository, usageLogRepository, new UsageMapper(),
                new SubscriptionGuard(), TestData.clock());
        freeSubscription = TestData.subscription(TestData.free());
    }

    @Test
    void consumeLogsUsageAndReturnsRemainingQuota() {
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(freeSubscription));
        when(usageLogRepository.sumQuotaUsed(10L, TestData.PERIOD)).thenReturn(40L);
        when(usageLogRepository.save(any(UsageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        UsageResponse response = service.consume(1L, new UsageRequest(" /v1/summarize ", 1));

        assertThat(response.usedThisMonth()).isEqualTo(41);
        assertThat(response.remaining()).isEqualTo(59);
        assertThat(response.monthlyLimit()).isEqualTo(100);
        ArgumentCaptor<UsageLog> captor = ArgumentCaptor.forClass(UsageLog.class);
        verify(usageLogRepository).save(captor.capture());
        assertThat(captor.getValue().getOperation()).isEqualTo("/v1/summarize");
        assertThat(captor.getValue().getUsagePeriod()).isEqualTo(TestData.PERIOD);
        assertThat(captor.getValue().getQuotaUsed()).isEqualTo(1);
    }

    @Test
    void consumeDefaultsToOneUnitWhenUnitsMissing() {
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(freeSubscription));
        when(usageLogRepository.sumQuotaUsed(10L, TestData.PERIOD)).thenReturn(0L);
        when(usageLogRepository.save(any(UsageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        UsageResponse response = service.consume(1L, new UsageRequest("/v1/ping", null));

        assertThat(response.unitsConsumed()).isEqualTo(1);
    }

    @Test
    void consumeAllowsReachingExactlyTheLimit() {
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(freeSubscription));
        when(usageLogRepository.sumQuotaUsed(10L, TestData.PERIOD)).thenReturn(99L);
        when(usageLogRepository.save(any(UsageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        UsageResponse response = service.consume(1L, new UsageRequest("/v1/ping", 1));

        assertThat(response.remaining()).isZero();
    }

    @Test
    void consumeRejectsWhenQuotaWouldBeExceeded() {
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(freeSubscription));
        when(usageLogRepository.sumQuotaUsed(10L, TestData.PERIOD)).thenReturn(100L);
        UsageRequest request = new UsageRequest("/v1/ping", 1);

        assertThatThrownBy(() -> service.consume(1L, request)).isInstanceOf(QuotaExceededException.class);
        verify(usageLogRepository, never()).save(any(UsageLog.class));
    }

    @Test
    void consumeRejectsExpiredSubscriptionAndMarksItExpired() {
        freeSubscription.expire(TestData.NOW.minusSeconds(60));
        when(subscriptionRepository.findWithLockByUserId(1L)).thenReturn(Optional.of(freeSubscription));
        UsageRequest request = new UsageRequest("/v1/ping", 1);

        assertThatThrownBy(() -> service.consume(1L, request)).isInstanceOf(SubscriptionExpiredException.class);
        assertThat(freeSubscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verifyNoInteractions(usageLogRepository);
    }

    @Test
    void consumeFailsWhenSubscriptionDoesNotExist() {
        when(subscriptionRepository.findWithLockByUserId(99L)).thenReturn(Optional.empty());
        UsageRequest request = new UsageRequest("/v1/ping", 1);

        assertThatThrownBy(() -> service.consume(99L, request)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void historyReturnsMappedPage() {
        UsageLog log = new UsageLog(freeSubscription, "/v1/ping", 2, TestData.PERIOD, TestData.NOW);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(freeSubscription));
        when(usageLogRepository.findBySubscriptionIdOrderByCreatedAtDescIdDesc(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 10), 1));

        PageResponse<UsageLogResponse> page = service.history(1L, 0, 10);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).quotaUsed()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void historyFailsWhenSubscriptionDoesNotExist() {
        when(subscriptionRepository.findByUserId(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.history(5L, 0, 10)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void dailyUsageAggregatesUnitsPerUtcDay() {
        UsageLog first = new UsageLog(freeSubscription, "/a", 2, TestData.PERIOD, Instant.parse("2026-10-01T10:00:00Z"));
        UsageLog second = new UsageLog(freeSubscription, "/b", 3, TestData.PERIOD, Instant.parse("2026-10-01T23:00:00Z"));
        UsageLog third = new UsageLog(freeSubscription, "/c", 4, TestData.PERIOD, Instant.parse("2026-10-02T01:00:00Z"));
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(freeSubscription));
        when(usageLogRepository.findBySubscriptionIdAndUsagePeriod(10L, TestData.PERIOD))
                .thenReturn(List.of(first, second, third));

        List<DailyUsageResponse> result = service.dailyUsage(1L);

        assertThat(result).containsExactly(
                new DailyUsageResponse(LocalDate.of(2026, 10, 1), 5L),
                new DailyUsageResponse(LocalDate.of(2026, 10, 2), 4L));
    }

    @Test
    void dailyUsageFailsWhenSubscriptionDoesNotExist() {
        when(subscriptionRepository.findByUserId(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.dailyUsage(8L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
