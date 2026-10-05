package com.saasplatform.quota.dto;

import com.saasplatform.quota.entity.PlanType;
import com.saasplatform.quota.entity.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "User summary including the current plan")
public record UserResponse(
        @Schema(description = "User id", example = "1") Long id,
        @Schema(description = "Full name", example = "Ayse Yilmaz") String fullName,
        @Schema(description = "E-mail", example = "ayse@example.com") String email,
        @Schema(description = "Registration time") Instant createdAt,
        @Schema(description = "Current plan", example = "FREE") PlanType planType,
        @Schema(description = "Current subscription status", example = "ACTIVE") SubscriptionStatus subscriptionStatus) {
}
