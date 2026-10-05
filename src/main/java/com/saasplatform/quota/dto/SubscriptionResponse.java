package com.saasplatform.quota.dto;

import com.saasplatform.quota.entity.PlanType;
import com.saasplatform.quota.entity.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Subscription state with current month quota usage")
public record SubscriptionResponse(
        @Schema(description = "Subscription id", example = "1") Long id,
        @Schema(description = "Owner user id", example = "1") Long userId,
        @Schema(description = "Plan type", example = "PRO") PlanType planType,
        @Schema(description = "Effective status (an elapsed term is reported as EXPIRED)", example = "ACTIVE") SubscriptionStatus status,
        @Schema(description = "Term start") Instant startDate,
        @Schema(description = "Term end") Instant endDate,
        @Schema(description = "Monthly limit of the current plan", example = "10000") int monthlyLimit,
        @Schema(description = "Quota used in the current month", example = "250") long usedThisMonth,
        @Schema(description = "Quota remaining in the current month", example = "9750") long remaining,
        @Schema(description = "Billing period key", example = "2026-10") String period) {
}
