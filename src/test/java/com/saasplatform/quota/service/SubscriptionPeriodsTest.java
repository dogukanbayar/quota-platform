package com.saasplatform.quota.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.saasplatform.quota.TestData;
import com.saasplatform.quota.util.SubscriptionPeriods;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SubscriptionPeriodsTest {

    @Test
    void currentPeriodUsesYearAndMonthInUtc() {
        assertThat(SubscriptionPeriods.currentPeriod(TestData.clock())).isEqualTo("2026-10");
    }

    @Test
    void termEndsOneCalendarMonthLater() {
        assertThat(SubscriptionPeriods.endFrom(Instant.parse("2026-01-31T00:00:00Z")))
                .isEqualTo(Instant.parse("2026-02-28T00:00:00Z"));
    }
}
