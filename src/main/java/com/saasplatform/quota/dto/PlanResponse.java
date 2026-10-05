package com.saasplatform.quota.dto;

import com.saasplatform.quota.entity.PlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "A subscription plan and its monthly quota")
public record PlanResponse(
        @Schema(description = "Plan id", example = "1") Long id,
        @Schema(description = "Plan type", example = "PRO") PlanType type,
        @Schema(description = "Monthly API call limit", example = "10000") int monthlyLimit,
        @Schema(description = "Monthly price in USD", example = "29.90") BigDecimal monthlyPrice,
        @Schema(description = "Marketing description") String description) {
}
