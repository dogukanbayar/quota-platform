package com.saasplatform.quota.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.saasplatform.quota.TestData;
import com.saasplatform.quota.dto.SubscriptionResponse;
import com.saasplatform.quota.dto.UserResponse;
import com.saasplatform.quota.entity.AppUser;
import com.saasplatform.quota.entity.Subscription;
import com.saasplatform.quota.entity.SubscriptionStatus;
import org.junit.jupiter.api.Test;

class MapperTest {

    @Test
    void remainingQuotaNeverGoesNegative() {
        Subscription subscription = TestData.subscription(TestData.free());

        SubscriptionResponse response = new SubscriptionMapper()
                .toResponse(subscription, SubscriptionStatus.ACTIVE, 150L, TestData.PERIOD);

        assertThat(response.remaining()).isZero();
        assertThat(response.userId()).isEqualTo(1L);
    }

    @Test
    void userWithoutSubscriptionHasNoPlan() {
        AppUser user = new AppUser("No Sub", "nosub@example.com", TestData.NOW);

        UserResponse response = new UserMapper().toResponse(user, TestData.NOW);

        assertThat(response.planType()).isNull();
        assertThat(response.subscriptionStatus()).isNull();
    }
}
