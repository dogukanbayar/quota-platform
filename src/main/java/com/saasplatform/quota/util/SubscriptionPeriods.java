package com.saasplatform.quota.util;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

public final class SubscriptionPeriods {

    private SubscriptionPeriods() {
    }

    /** Billing month key (yyyy-MM, UTC) for the given clock. */
    public static String currentPeriod(Clock clock) {
        return YearMonth.now(clock.withZone(ZoneOffset.UTC)).toString();
    }

    /** A subscription term lasts exactly one calendar month from its start. */
    public static Instant endFrom(Instant start) {
        return start.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
    }
}
