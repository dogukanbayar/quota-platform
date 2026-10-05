package com.saasplatform.quota;

import com.saasplatform.quota.entity.AppUser;
import com.saasplatform.quota.entity.Plan;
import com.saasplatform.quota.entity.PlanType;
import com.saasplatform.quota.entity.Subscription;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.test.util.ReflectionTestUtils;

/** Shared fixtures for unit tests. */
public final class TestData {

    public static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");
    public static final String PERIOD = "2026-10";

    private TestData() {
    }

    public static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    public static Plan plan(PlanType type, int limit) {
        return new Plan(type, limit, BigDecimal.TEN, type + " plan");
    }

    public static Plan free() {
        return plan(PlanType.FREE, 100);
    }

    public static Plan pro() {
        return plan(PlanType.PRO, 10_000);
    }

    /** User id 1, subscription id 10, active for 30 days from NOW. */
    public static Subscription subscription(Plan plan) {
        AppUser user = new AppUser("Test User", "test@example.com", NOW);
        ReflectionTestUtils.setField(user, "id", 1L);
        Subscription subscription = new Subscription(user, plan, NOW, NOW.plusSeconds(30L * 24 * 3600));
        ReflectionTestUtils.setField(subscription, "id", 10L);
        user.setSubscription(subscription);
        return subscription;
    }
}
