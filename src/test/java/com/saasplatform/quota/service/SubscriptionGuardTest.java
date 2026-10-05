package com.saasplatform.quota.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.saasplatform.quota.TestData;
import com.saasplatform.quota.entity.Subscription;
import com.saasplatform.quota.exception.SubscriptionExpiredException;
import org.junit.jupiter.api.Test;

class SubscriptionGuardTest {

    private final SubscriptionGuard guard = new SubscriptionGuard();

    @Test
    void activeSubscriptionPasses() {
        Subscription subscription = TestData.subscription(TestData.free());

        assertThatCode(() -> guard.assertActive(subscription, TestData.NOW)).doesNotThrowAnyException();
    }

    @Test
    void subscriptionPastItsEndDateIsRejected() {
        Subscription elapsed = TestData.subscription(TestData.free());
        elapsed.renew(TestData.NOW.minusSeconds(10), TestData.NOW);

        assertThatThrownBy(() -> guard.assertActive(elapsed, TestData.NOW)).isInstanceOf(SubscriptionExpiredException.class);
    }
}
